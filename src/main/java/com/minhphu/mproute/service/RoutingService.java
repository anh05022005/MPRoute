package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Depot;
import com.minhphu.mproute.entity.OptimizationResult;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.repository.OptimizationResultRepository;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import com.minhphu.mproute.service.optimization.DistanceMatrixService;
import com.minhphu.mproute.service.optimization.EtaCalculator;
import com.minhphu.mproute.service.optimization.MetricsService;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import com.minhphu.mproute.service.optimization.RoutingResult;
import com.minhphu.mproute.service.optimization.ortools.OrToolsSolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chay thuat toan phan tuyen roi luu ket qua xuong CSDL (CN-03-06, CN-03-12, CN-03-13).
 *
 * DAY LA LOP NOI GIUA THUAT TOAN VA PHAN CON LAI CUA HE THONG. Ranh gioi rat ro:
 *
 *   service/optimization/   khong biet gi ve JPA, chi lam viec voi mang double[][]
 *   RoutingService          doc CSDL -> dung dau vao -> goi thuat toan -> ghi CSDL
 *   RoutingController       chi nhan tham so tu form va do ket qua ra man hinh
 *
 * Nho ranh gioi nay ma thuat toan van chay duoc bang main() khong can MySQL
 * (AlgorithmRunner), va van chay duoc bo benchmark Augerat o tuan 10.
 *
 * DIEU CAM SO 4 duoc bao dam o day: ma tran khoang cach nap MOT lan o buoc B5, sau do
 * bo giai chi tra matrix[i][j]. Trong ca lop nay khong co mot cau truy van hay mot loi
 * goi OSRM nao nam ben trong vong lap cua thuat toan.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    /** Ma tuyen: TT + yyMMdd + 2 chu so thu tu (docs/02 muc 10). */
    private static final DateTimeFormatter ROUTE_CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private static final int MAX_ROUTES_PER_DAY = 99;

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final OptimizationResultRepository optimizationResultRepository;
    private final UserRepository userRepository;

    private final DepotService depotService;
    private final VehicleService vehicleService;
    private final DistanceMatrixService distanceMatrixService;
    private final SystemParameterService systemParameterService;
    private final StatusHistoryService statusHistoryService;

    /**
     * Ket qua mot lan chay, du de man hinh phan tuyen do ra bang tuyen (CN-03-07),
     * bang chi so so sanh (CN-03-08) va khoi canh bao don chua gan (CN-03-14).
     *
     * unassignedOrders RONG chu khong bao gio null — thuat toan bo sot don thi phai noi
     * ra bang danh sach cu the, khong duoc im lang (BR03-13).
     */
    public record RoutingOutcome(List<Route> routes,
                                 List<Order> unassignedOrders,
                                 OptimizationResult metrics) {

        public boolean hasUnassignedOrders() {
            return !unassignedOrders.isEmpty();
        }
    }

    // ==================== Duong vao chinh ====================

    /**
     * Chay phan tuyen cho mot ngay giao.
     *
     * CA HAM NAY NAM TRONG MOT GIAO DICH. Day khong phai chi tiet trang tri: buoc B1 xoa
     * ket qua nhap cua lan chay truoc, ma buoc B4 co the nem loi thieu suc chua. Nam chung
     * mot giao dich thi lan chay hong se hoan tac ca buoc xoa — dieu phoi vien chon thieu
     * xe khong lam mat ket qua dang co tren man hinh.
     *
     * @param vehicleIds    cac xe dieu phoi vien tich chon (BR03-02)
     * @param currentUserId nguoi bam nut, de ghi vao created_by va status_history
     */
    @Transactional
    public RoutingOutcome optimize(LocalDate deliveryDate,
                                   List<Long> vehicleIds,
                                   Long currentUserId) {

        // ---------- B1: xoa ket qua DRAFT cua lan chay truoc (BR03-09, CN-03-12) ----------
        clearDraftRoutes(deliveryDate, currentUserId);

        // ---------- B2: don da duyet cua ngay do (BR03-01) ----------
        List<Order> orders = orderRepository.findByDeliveryDateAndStatus(deliveryDate, OrderStatus.APPROVED);
        if (orders.isEmpty()) {
            throw new IllegalStateException("Ngày này chưa có đơn nào đã duyệt nên chưa phân tuyến được.");
        }

        // ---------- B3: xe duoc chon (BR03-02) ----------
        List<Vehicle> vehicles = loadSelectedVehicles(vehicleIds, deliveryDate);

        // ---------- B4: tong nhu cau so voi tong suc chua (BR03-04, MSG-08) ----------
        checkTotalCapacity(orders, vehicles);

        // ---------- B5: nap ma tran MOT lan vao bo nho (BR03-06, dieu cam so 4) ----------
        Depot depot = depotService.getActiveDepot();
        List<OptimizationPoint> allPoints = buildPoints(depot, orders);
        DistanceMatrixService.DistanceMatrix matrix = distanceMatrixService.loadMatrix(allPoints);

        // ---------- B6: chay bo giai OR-Tools (CN-03-06, BR03-10) ----------
        // He thong chi con mot bo giai, khong cho chon — xem docs/09 muc A7
        OrToolsSolver solver = new OrToolsSolver();

        /*
         * Diem giao = toan bo danh sach TRU kho o dau. Ban sao moi vi cac bo giai xoa dan
         * phan tu khoi danh sach chung dang duyet.
         */
        List<OptimizationPoint> deliveryPoints = new ArrayList<>(allPoints.subList(1, allPoints.size()));

        RoutingResult result = solver.solve(deliveryPoints, toOptimizationVehicles(vehicles), matrix.meters());
        result.setApproximateDistance(matrix.approximate());

        // ---------- B7: hai moc so sanh va gio du kien den (CN-03-08, CBR-13) ----------
        MetricsService.fillBaselines(result, matrix.meters());
        assignEta(result, matrix.meters(), deliveryDate);

        // ---------- B8: ghi tuyen xuong CSDL, don chuyen sang ROUTED (CBR-14) ----------
        List<Route> savedRoutes = saveRoutes(result, deliveryDate, depot, orders, vehicles,
                matrix.meters(), currentUserId);

        // ---------- B9: luu chi so cua lan chay (CN-03-13, CBR-18) ----------
        OptimizationResult metrics = saveMetrics(result, deliveryDate, savedRoutes);

        List<Order> unassignedOrders = unassignedOrdersOf(result, orders);
        log.info("Phan tuyen ngay {}: {} tuyen, {} don duoc gan, {} don con sot, {} ms",
                deliveryDate, savedRoutes.size(),
                orders.size() - unassignedOrders.size(), unassignedOrders.size(),
                result.getExecutionTimeMs());

        return new RoutingOutcome(savedRoutes, unassignedOrders, metrics);
    }

    // ==================== B1 ====================

    /**
     * Xoa cac tuyen DRAFT cua ngay do va tra don ve APPROVED (BR03-09, CN-03-12).
     *
     * CHI dung toi tuyen DRAFT. Tuyen da CHOT thi don da sang ROUTED/DELIVERING, khong con
     * nam trong tap don APPROVED nen khong bao gio bi lan chay moi dung toi.
     *
     * Tra don ve APPROVED truoc khi xoa la bat buoc: xoa tuyen ma de don o ROUTED thi don
     * do bien mat khoi ca hai man hinh — khong con tuyen nao chua no, ma cung khong duoc
     * lan phan tuyen sau nhat len.
     */
    private void clearDraftRoutes(LocalDate deliveryDate, Long currentUserId) {
        List<Route> draftRoutes = routeRepository.findByDeliveryDateAndStatus(deliveryDate, RouteStatus.DRAFT);
        if (draftRoutes.isEmpty()) {
            return;
        }

        for (Route route : draftRoutes) {
            for (RouteStop stop : route.getStops()) {
                Order order = stop.getOrder();
                if (order.getStatus() == OrderStatus.ROUTED) {
                    order.setStatus(OrderStatus.APPROVED);
                    orderRepository.save(order);
                    statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                            OrderStatus.ROUTED.name(), OrderStatus.APPROVED.name(),
                            currentUserId, "Chay lai phan tuyen, huy ket qua nhap cu");
                }
            }
        }

        // orphanRemoval tren Route.stops nen xoa tuyen la xoa luon cac diem dung
        routeRepository.deleteAll(draftRoutes);
        routeRepository.flush();
        log.info("Chay lai phan tuyen ngay {}: da xoa {} tuyen nhap cu", deliveryDate, draftRoutes.size());
    }

    // ==================== B3 ====================

    /**
     * Lay cac xe dieu phoi vien tich chon, chi nhan xe ranh trong CHINH ngay giao (CBR-05, BR03-02).
     *
     * Kiem lai o day chu khong tin vao danh sach o chon: hoi dong se gui thang id cua mot
     * xe da co tuyen trong ngay len xem he thong co nhan khong.
     */
    private List<Vehicle> loadSelectedVehicles(List<Long> vehicleIds, LocalDate deliveryDate) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn xe nào để phân tuyến.");
        }

        List<Vehicle> vehicles = vehicleRepository.findAllById(vehicleIds);
        if (vehicles.size() != vehicleIds.size()) {
            throw new IllegalArgumentException("Có xe được chọn không còn tồn tại trong danh mục.");
        }

        for (Vehicle vehicle : vehicles) {
            vehicleService.checkSchedulableOn(vehicle, deliveryDate);
        }
        return vehicles;
    }

    // ==================== B4 ====================

    /**
     * Tong nhu cau vuot tong suc chua thi bao MSG-08 va KHONG sinh tuyen nao (BR03-04, CN-03-10).
     *
     * Kiem CA HAI chieu kg va m3 trong cung mot lan (CBR-03, CBR-04). Hang cua Minh Phu co
     * loai nhe ma cong kenh, nen mot doi xe du tai trong van co the thieu the tich — chi kiem
     * kg thi he thong bao du roi thuat toan lai bo sot mot dong don.
     *
     * Day la phep kiem TONG, va no chi chan duoc truong hop chac chan hong. Tong du ma van
     * khong xep het don la chuyen binh thuong cua bai toan xep thung — truong hop do xu ly
     * rieng o CN-03-14, khong bao loi o day.
     */
    private void checkTotalCapacity(List<Order> orders, List<Vehicle> vehicles) {
        double requiredWeight = orders.stream().mapToDouble(Order::getTotalWeight).sum();
        double requiredVolume = orders.stream().mapToDouble(Order::getTotalVolume).sum();

        double availableWeight = vehicles.stream().mapToDouble(Vehicle::getMaxWeight).sum();
        double availableVolume = vehicles.stream().mapToDouble(Vehicle::getMaxVolume).sum();

        double missingWeight = requiredWeight - availableWeight;
        double missingVolume = requiredVolume - availableVolume;

        if (missingWeight > 0 || missingVolume > 0) {
            // MSG-08 — neu ro phan con thieu de dieu phoi vien biet phai chon them xe co gi
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Tổng sức chứa không đủ. Còn thiếu %.1f kg / %.3f m³. "
                            + "Nhu cầu %.1f kg / %.3f m³, sức chứa đã chọn %.1f kg / %.3f m³.",
                    Math.max(missingWeight, 0), Math.max(missingVolume, 0),
                    requiredWeight, requiredVolume, availableWeight, availableVolume));
        }
    }

    // ==================== B5 ====================

    /**
     * Doi don hang sang diem cua bai toan.
     *
     * KHO LUON DUNG DAU danh sach — quy uoc xuyen suot do an: chi so 0 trong ma tran la kho.
     * DistanceMatrixService dua vao dung thu tu nay de danh matrixIndex.
     */
    private List<OptimizationPoint> buildPoints(Depot depot, List<Order> orders) {
        List<OptimizationPoint> points = new ArrayList<>();
        points.add(OptimizationPoint.depot(depot.getLatitude(), depot.getLongitude()));

        for (Order order : orders) {
            points.add(OptimizationPoint.forOrder(
                    order.getId(),
                    order.getCustomer().getName(),
                    order.getLatitude(),
                    order.getLongitude(),
                    order.getTotalWeight(),
                    order.getTotalVolume()));
        }
        return points;
    }

    /** Doi xe trong danh muc sang xe cua bai toan — thuat toan chi can bon con so. */
    private List<OptimizationVehicle> toOptimizationVehicles(List<Vehicle> vehicles) {
        List<OptimizationVehicle> converted = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            converted.add(new OptimizationVehicle(vehicle.getId(), vehicle.getLicensePlate(),
                    vehicle.getMaxWeight(), vehicle.getMaxVolume()));
        }
        return converted;
    }

    // ==================== B7 ====================

    /**
     * Gio du kien den tung diem (CBR-13).
     *
     * Ba tham so lay tu bang system_parameters chu khong hard-code: doi AVERAGE_SPEED o man
     * hinh cai dat roi chay lai phan tuyen thi ETA phai doi theo — day chinh la phep nghiem
     * thu cua CN-01-08.
     */
    private void assignEta(RoutingResult result, double[][] matrix, LocalDate deliveryDate) {
        double speedKmh = systemParameterService.getDouble(SystemParameterService.AVERAGE_SPEED);
        int loadingMinutes = systemParameterService.getInt(SystemParameterService.LOADING_TIME);

        EtaCalculator.assignEta(result.getRoutes(), matrix,
                plannedDepartureOn(deliveryDate), speedKmh, loadingMinutes);
    }

    /**
     * Tinh lai quang duong va ETA cho cac tuyen ma THU TU DIEM DA DUOC AN DINH san
     * (CN-04-01, CN-04-02, BR04-03).
     *
     * Dung sau khi dieu phoi vien keo tha chinh tay: luc do khong con bai toan toi uu nao
     * de giai nua, chi con phai do lai xem cach sap moi dai bao nhieu km va den tung diem
     * luc may gio.
     *
     * DUNG LAI DUNG HAI HAM CUA THUAT TOAN — RouteDistanceCalculator va EtaCalculator —
     * chu khong viet mot cong thuc thu hai. Neu tinh rieng o day thi chi can quen chang
     * cuoi ve kho (CBR-01) la con so cua phuong an chinh tay va con so cua phuong an tu
     * dong khong con so sanh duoc voi nhau nua, ma bang so sanh CN-04-04 dat hai con so
     * do canh nhau.
     *
     * Ma tran nap MOT lan cho toan bo diem cua ca ngay roi dung chung — dung nguyen tac
     * cua dieu cam so 4, khong tra khoang cach tung cap mot trong vong lap.
     */
    @Transactional
    public void recalculate(List<Route> routes) {
        if (routes.isEmpty()) {
            return;
        }

        Depot depot = depotService.getActiveDepot();

        /*
         * Gom diem cua TAT CA cac tuyen vao mot ma tran chung. Moi tuyen mot ma tran rieng
         * thi khong tinh duoc chang giua hai tuyen, ma keo tha lai chuyen don qua lai giua
         * chung — dung chung mot ma tran thi don sang tuyen nao cung co san khoang cach.
         */
        List<OptimizationPoint> allPoints = new ArrayList<>();
        allPoints.add(OptimizationPoint.depot(depot.getLatitude(), depot.getLongitude()));

        Map<Long, OptimizationPoint> pointByOrderId = new LinkedHashMap<>();
        for (Route route : routes) {
            for (RouteStop stop : route.getStops()) {
                Order order = stop.getOrder();
                OptimizationPoint point = OptimizationPoint.forOrder(
                        order.getId(), order.getCustomer().getName(),
                        order.getLatitude(), order.getLongitude(),
                        order.getTotalWeight(), order.getTotalVolume());
                pointByOrderId.put(order.getId(), point);
                allPoints.add(point);
            }
        }

        DistanceMatrixService.DistanceMatrix matrix = distanceMatrixService.loadMatrix(allPoints);
        double[][] meters = matrix.meters();

        double speedKmh = systemParameterService.getDouble(SystemParameterService.AVERAGE_SPEED);
        int loadingMinutes = systemParameterService.getInt(SystemParameterService.LOADING_TIME);

        for (Route route : routes) {
            if (route.getStops().isEmpty()) {
                route.setTotalDistance(0.0);
                route.setTotalWeight(0.0);
                route.setTotalVolume(0.0);
                continue;
            }

            // Dung lai doi tuong cua thuat toan de goi duoc dung hai ham tinh cua no
            OptimizationRoute optimizationRoute = new OptimizationRoute(new OptimizationVehicle(
                    route.getVehicle().getId(), route.getVehicle().getLicensePlate(),
                    route.getVehicle().getMaxWeight(), route.getVehicle().getMaxVolume()));

            for (RouteStop stop : route.getStops()) {
                optimizationRoute.addPoint(pointByOrderId.get(stop.getOrder().getId()));
            }

            RouteDistanceCalculator.updateTotalDistance(optimizationRoute, meters);

            /*
             * Xe da roi kho thi lay gio roi kho THAT lam moc (docs/09 muc B7): tai xe nhan hang luc
             * 14:00 ma van tinh tu 08:00 thi khach doc ETA da qua tu sang. Cong thuc ETA giu
             * nguyen, chi doi moc xuat phat.
             */
            LocalDateTime departure = (route.getDepartedAt() != null)
                    ? route.getDepartedAt()
                    : plannedDepartureOn(route.getDeliveryDate());
            EtaCalculator.assignEta(optimizationRoute, meters, departure, speedKmh, loadingMinutes);

            route.setTotalDistance(optimizationRoute.getTotalDistance());
            route.setTotalWeight(optimizationRoute.getTotalWeight());
            route.setTotalVolume(optimizationRoute.getTotalVolume());
            route.setApproximateDistance(matrix.approximate());

            // Chep nguoc ket qua vao tung diem dung: chang toi diem nay va gio du kien den
            List<OptimizationPoint> points = optimizationRoute.getPoints();
            for (int i = 0; i < points.size(); i++) {
                RouteStop stop = route.getStops().get(i);
                int previousIndex = (i == 0)
                        ? RouteDistanceCalculator.DEPOT_INDEX
                        : points.get(i - 1).getMatrixIndex();

                stop.setLegDistance(meters[previousIndex][points.get(i).getMatrixIndex()]);
                stop.setEstimatedArrival(points.get(i).getEstimatedArrival());
            }
        }

        routeRepository.saveAll(routes);
    }

    /**
     * Ma tran khoang cach cua toan bo diem trong ngay, de trinh duyet tinh THU quang duong
     * ngay luc keo tha (CN-04-01, BR04-03).
     *
     * Con so trinh duyet tinh ra chi la XEM TRUOC. Luc bam Luu thi may chu tinh lai bang
     * recalculate() va con so cua may chu moi la con so duoc ghi xuong CSDL — trinh duyet
     * khong bao gio duoc quyen quyet dinh mot con so nghiep vu.
     */
    @Transactional
    public DistanceMatrixService.DistanceMatrix matrixFor(List<OptimizationPoint> points) {
        return distanceMatrixService.loadMatrix(points);
    }

    /**
     * Gio xe du kien roi kho cua mot ngay giao, dung lam moc tinh ETA cho tuyen chua xuat phat.
     *
     * Binh thuong la tham so DEPARTURE_TIME. Rieng tuyen cua HOM NAY ma xep sau gio do (don
     * phat sinh trong ngay, chuyen thu hai cua tai xe) thi xe som nhat cung chi di duoc tu bay
     * gio — lay 08:00 thi email bao khach mot gio da troi qua.
     */
    public LocalDateTime plannedDepartureOn(LocalDate deliveryDate) {
        LocalDateTime fromParameter = LocalDateTime.of(deliveryDate, departureTime());
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (deliveryDate.equals(now.toLocalDate()) && now.isAfter(fromParameter)) {
            return now;
        }
        return fromParameter;
    }

    /** Gio xe roi kho, tham so DEPARTURE_TIME dang HH:mm. */
    public LocalTime departureTime() {
        String value = systemParameterService.getValue(SystemParameterService.DEPARTURE_TIME);
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException ex) {
            log.warn("Tham so DEPARTURE_TIME dang la '{}', khong doc duoc. Dung 08:00.", value);
            return LocalTime.of(8, 0);
        }
    }

    // ==================== B8 ====================

    /**
     * Ghi ket qua thuat toan xuong bang routes va route_stops.
     *
     * Tuyen luu o trang thai DRAFT: dieu phoi vien con phai xem lai, chinh tay, gan tai xe
     * roi moi chot (CN-04-07). Xe van giu AVAILABLE cho toi luc chot (BR03-12).
     */
    private List<Route> saveRoutes(RoutingResult result,
                                   LocalDate deliveryDate,
                                   Depot depot,
                                   List<Order> orders,
                                   List<Vehicle> vehicles,
                                   double[][] matrix,
                                   Long currentUserId) {

        Map<Long, Order> ordersById = indexOrders(orders);

        Map<Long, Vehicle> vehiclesById = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicles) {
            vehiclesById.put(vehicle.getId(), vehicle);
        }

        User createdBy = (currentUserId == null) ? null : userRepository.findById(currentUserId).orElse(null);
        String codePrefix = "TT" + deliveryDate.format(ROUTE_CODE_DATE);
        int sequenceOfDay = 0;

        List<Route> savedRoutes = new ArrayList<>();
        for (OptimizationRoute optimizationRoute : result.getRoutes()) {

            // Xe khong duoc giao diem nao thi khong sinh tuyen rong (BR03-12)
            if (optimizationRoute.isEmpty()) {
                continue;
            }

            sequenceOfDay++;
            Route route = new Route();
            route.setCode(nextRouteCode(codePrefix, sequenceOfDay));
            route.setDepot(depot);
            route.setVehicle(vehiclesById.get(optimizationRoute.getVehicle().getVehicleId()));
            route.setDeliveryDate(deliveryDate);
            route.setTotalDistance(optimizationRoute.getTotalDistance());
            route.setTotalWeight(optimizationRoute.getTotalWeight());
            route.setTotalVolume(optimizationRoute.getTotalVolume());
            route.setAlgorithm(result.getAlgorithm());
            route.setExecutionTimeMs(result.getExecutionTimeMs());
            route.setApproximateDistance(result.isApproximateDistance());
            route.setStatus(RouteStatus.DRAFT);
            route.setCreatedBy(createdBy);

            addStops(route, optimizationRoute, ordersById, matrix);
            savedRoutes.add(routeRepository.save(route));
        }

        routeRepository.flush();

        // Ghi lich su SAU khi tuyen da co id, de ghi chu neu duoc ma tuyen (CBR-14)
        for (Route route : savedRoutes) {
            for (RouteStop stop : route.getStops()) {
                Order order = stop.getOrder();
                order.setStatus(OrderStatus.ROUTED);
                orderRepository.save(order);
                statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                        OrderStatus.APPROVED.name(), OrderStatus.ROUTED.name(),
                        currentUserId, "Phan tuyen " + route.getCode());
            }
            // Tuyen vua sinh ra cung la mot lan chuyen trang thai, phai co dong lich su
            statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                    null, RouteStatus.DRAFT.name(),
                    currentUserId, "Sinh tuyen bang " + result.getAlgorithm().name());
        }

        return savedRoutes;
    }

    /**
     * Dung cac diem dung cua mot tuyen.
     *
     * legDistance la quang duong tu diem TRUOC toi diem nay, diem dau tien thi tinh tu kho.
     * Cong don legDistance cua ca tuyen se THIEU chang cuoi ve kho so voi totalDistance —
     * dung nhu vay, vi chang ve kho khong thuoc ve diem dung nao (CBR-01). Man hinh ket qua
     * lay totalDistance cua tuyen chu dung tu cong lai cac chang.
     */
    private void addStops(Route route, OptimizationRoute optimizationRoute,
                          Map<Long, Order> ordersById, double[][] matrix) {

        List<OptimizationPoint> points = optimizationRoute.getPoints();
        for (int i = 0; i < points.size(); i++) {
            OptimizationPoint point = points.get(i);

            int previousIndex = (i == 0)
                    ? RouteDistanceCalculator.DEPOT_INDEX
                    : points.get(i - 1).getMatrixIndex();

            RouteStop stop = new RouteStop();
            stop.setOrder(ordersById.get(point.getOrderId()));
            stop.setSequence(i + 1);          // tai xe dem tu 1 chu khong tu 0
            stop.setLegDistance(matrix[previousIndex][point.getMatrixIndex()]);
            stop.setEstimatedArrival(point.getEstimatedArrival());
            route.addStop(stop);
        }
    }

    /**
     * Ma tuyen TT + yyMMdd + 2 chu so.
     *
     * Van phai tra trung giong ma don: tuyen da CHOT cua lan chay truoc khong bi xoa, nen
     * so thu tu bat dau lai tu 1 co the dam vao ma da phat.
     */
    private String nextRouteCode(String codePrefix, int startFrom) {
        for (int sequence = startFrom; sequence <= MAX_ROUTES_PER_DAY; sequence++) {
            String code = String.format("%s%02d", codePrefix, sequence);
            if (routeRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Đã hết số thứ tự mã tuyến cho ngày này.");
    }

    // ==================== B9 ====================

    /**
     * Luu chi so cua lan chay (CN-03-13). Day la nguon du lieu duy nhat cua bao cao RPT-03,
     * va chi so KHONG BAO GIO duoc tinh lai ve sau (CBR-18): khoang cach, danh muc xe, du
     * lieu don deu co the doi, tinh lai se ra con so khac han.
     */
    private OptimizationResult saveMetrics(RoutingResult result,
                                           LocalDate deliveryDate,
                                           List<Route> savedRoutes) {

        OptimizationResult metrics = new OptimizationResult();
        metrics.setDeliveryDate(deliveryDate);
        metrics.setOrderCount(result.assignedPointCount());
        metrics.setRouteCount(savedRoutes.size());

        /*
         * "So xe su dung" theo docs/02 la so xe THUC SU nhan tuyen, khong phai so xe dieu
         * phoi vien tich chon: xe thua khong duoc gan tuyen thi giu AVAILABLE va khong tinh
         * vao day (BR03-12). Trong do an moi xe chi chay mot tuyen mot ngay nen con so nay
         * bang routeCount; van tach hai cot vi bang duoc thiet ke de sau nay con ghi duoc
         * truong hop mot xe chay hai chuyen.
         */
        metrics.setVehicleCount(savedRoutes.size());

        metrics.setBaselineADistance(result.getBaselineADistance());
        metrics.setBaselineBDistance(result.getBaselineBDistance());
        metrics.setOptimizedDistance(result.getOptimizedDistance());

        metrics.setReductionAMeters(result.getBaselineADistance() - result.getOptimizedDistance());
        metrics.setReductionAPercent(MetricsService.reductionPercent(
                result.getBaselineADistance(), result.getOptimizedDistance()));
        metrics.setReductionBMeters(result.getBaselineBDistance() - result.getOptimizedDistance());
        metrics.setReductionBPercent(MetricsService.reductionPercent(
                result.getBaselineBDistance(), result.getOptimizedDistance()));

        // Luon bao cao ca hai ty le lap day: xe day kg ma trong m3 va nguoc lai deu la thong tin
        metrics.setWeightUtilization(MetricsService.averageWeightUtilization(result.getRoutes()));
        metrics.setVolumeUtilization(MetricsService.averageVolumeUtilization(result.getRoutes()));

        metrics.setAlgorithm(result.getAlgorithm());
        metrics.setExecutionTimeMs(result.getExecutionTimeMs());
        metrics.setApproximateDistance(result.isApproximateDistance());

        return optimizationResultRepository.save(metrics);
    }

    // ==================== Ham dung chung ====================

    /**
     * Doi cac diem thuat toan khong xep duoc thanh danh sach don, de man hinh liet ke ma don
     * va khach cu the (CN-03-14, MSG-16).
     */
    private List<Order> unassignedOrdersOf(RoutingResult result, List<Order> orders) {
        Map<Long, Order> ordersById = indexOrders(orders);

        List<Order> unassigned = new ArrayList<>();
        for (OptimizationPoint point : result.getUnassignedPoints()) {
            Order order = ordersById.get(point.getOrderId());
            if (order != null) {
                unassigned.add(order);
            }
        }
        return unassigned;
    }

    /** Tra don theo id trong bo nho — vong lap ghi tuyen khong duoc truy van lai CSDL. */
    private Map<Long, Order> indexOrders(List<Order> orders) {
        Map<Long, Order> ordersById = new LinkedHashMap<>();
        for (Order order : orders) {
            ordersById.put(order.getId(), order);
        }
        return ordersById;
    }

    /**
     * Cac tuyen con dang song cua mot ngay (nhap va da chot), kem san diem dung.
     *
     * Sap theo ma tuyen de thu tu tren man hinh khong doi giua hai lan tai trang — ma tuyen
     * gan lien voi mau ve tren ban do, thu tu nhay thi mau cung nhay theo.
     */
    @Transactional(readOnly = true)
    public List<Route> visibleRoutesOf(LocalDate deliveryDate) {
        List<Route> routes = routeRepository.findWithStopsByDeliveryDateAndStatusIn(
                deliveryDate, List.of(RouteStatus.DRAFT, RouteStatus.CONFIRMED));
        routes.sort(Comparator.comparing(Route::getCode));
        return routes;
    }

    /**
     * Chi so cua lan chay GAN NHAT trong ngay, de do ra bang so sanh (CN-03-08).
     *
     * Moi lan chay them mot dong chu khong ghi de dong cu: bang nay la du lieu goc cua
     * bao cao RPT-03, chay lai ba lan trong ngay thi ca ba deu la so lieu that. Man hinh
     * chi hien dong moi nhat, con bao cao doc het.
     */
    @Transactional(readOnly = true)
    public OptimizationResult latestMetricsOf(LocalDate deliveryDate) {
        return optimizationResultRepository.findByDeliveryDate(deliveryDate).stream()
                .max(Comparator.comparing(OptimizationResult::getId))
                .orElse(null);
    }
}
