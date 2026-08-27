package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Incident;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.SolutionType;
import com.minhphu.mproute.entity.enums.StopStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import com.minhphu.mproute.service.optimization.DistanceMatrixService;
import com.minhphu.mproute.service.optimization.EtaCalculator;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import com.minhphu.mproute.service.optimization.heuristic.CheapestInsertion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sinh ba nhom phuong an xu ly su co, xep theo km phat sinh (CN-06-07 -> CN-06-10).
 *
 * KM PHAT SINH LA THUOC DO DUY NHAT DUNG DE XEP HANG, va no phai duoc tinh giong nhau cho ca
 * ba phuong an, neu khong thi bang xep hang vo nghia. Dinh nghia thong nhat o day:
 *
 *     km phat sinh = so met he thong phai chay THEM so voi truong hop khong co su co
 *
 * Voi phuong an A do la ca chuyen cua xe cuu ho (kho -> diem su co -> cac diem con lai -> kho).
 * Voi phuong an B do la tong chi phi chen cac diem vao tuyen khac. Voi phuong an C thi bang 0,
 * nhung C VAN LUON XEP CUOI (BR06-08, BR06-11): hoan la khong ai phai chay them met nao, nhung
 * doi lai khach khong nhan duoc hang hom nay — mot cai gia khong do bang km duoc.
 *
 * MOI CON SO O DAY DEU TU MOT MA TRAN CHUNG, nap mot lan cho toan bo diem lien quan. Moi phuong
 * an mot ma tran rieng thi khong so sanh duoc voi nhau, ma bang xep hang lai chinh la san pham
 * cua lop nay.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentSolutionService {

    /** Chi so quy uoc trong ma tran dung rieng cho bai toan su co. */
    private static final int DEPOT_INDEX = 0;
    private static final int INCIDENT_INDEX = 1;

    /*
     * Chuoi hien ra man hinh phai theo quy uoc tieng Viet: dau phay thap phan, dau cham hang
     * nghin (docs/05). Locale.ROOT se ra "1000 kg / 7.0 m3" — dung cu phap Java nhung sai quy
     * uoc hien thi, va nhin canh cac con so khac tren cung man hinh la thay lech ngay.
     */
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DistanceMatrixService distanceMatrixService;
    private final SystemParameterService systemParameterService;

    /** Gio du kien moi cua mot khach, de hien trong tung phuong an (BR06-10). */
    public record StopEta(String customerName, String orderCode, LocalDateTime estimatedArrival) {}

    /**
     * Mot phuong an xu ly.
     *
     * @param targetVehicleId xe cuu ho, chi co o phuong an A
     * @param targetRouteId   tuyen nhan them diem, chi co o phuong an B
     * @param resourceNeeded  nguon luc phai huy dong, cau chu de dieu phoi vien can nhac (BR06-10)
     */
    public record SolutionOption(SolutionType type,
                                 String title,
                                 double extraDistanceMeters,
                                 String resourceNeeded,
                                 Long targetVehicleId,
                                 Long targetRouteId,
                                 List<StopEta> newEtas) {}

    /**
     * Sinh toan bo phuong an kha thi cho mot su co, da xep hang (BR06-05, BR06-09, BR06-10).
     *
     * CHI TRA VE PHUONG AN KHA THI. Phuong an vi pham suc chua bi loai ngay tai day chu khong
     * hien ra roi mo ra moi bao khong dung duoc — dieu phoi vien dang xu ly su co, khong co thoi
     * gian thu tung cai mot.
     */
    /*
     * KHONG dat readOnly = true du ham nay chi "doc" theo nghia nghiep vu: loadMatrix() co GHI
     * — no luu cac o khoang cach vua lay tu OSRM vao bang distance_matrix lam bo nho dem (CBR-12).
     * Dat readOnly thi MySQL tu choi thang cau INSERT do voi loi "Connection is read-only",
     * va man hinh su co tra ve 500 dung luc dang can no nhat.
     */
    @Transactional
    public List<SolutionOption> generateOptions(Incident incident) {
        Route brokenRoute = incident.getRoute();
        List<RouteStop> remainingStops = remainingStopsOf(brokenRoute);

        List<SolutionOption> options = new ArrayList<>();
        if (remainingStops.isEmpty()) {
            // Su co xay ra sau khi da giao het: khong con gi de cuu, chi con dong su co lai
            return options;
        }

        List<Route> candidateRoutes = candidateRoutesFor(brokenRoute);

        // Mot ma tran chung cho tat ca: kho, diem su co, diem con lai, va diem cua cac tuyen ung vien
        MatrixContext context = buildMatrix(incident, brokenRoute, remainingStops, candidateRoutes);

        buildRescueOption(remainingStops, context).ifPresent(options::add);
        options.addAll(buildInsertionOptions(remainingStops, candidateRoutes, context));

        // BR06-05, BR06-10 — xep theo km phat sinh tang dan
        options.sort(Comparator.comparingDouble(SolutionOption::extraDistanceMeters));

        // BR06-08, BR06-11 — phuong an C luon hien va luon xep cuoi, ke ca khi km phat sinh la 0
        options.add(buildPostponeOption(remainingStops));
        return options;
    }

    // ==================== CN-06-07 phuong an A ====================

    /**
     * Dieu mot xe AVAILABLE tu kho ra sang hang roi chay not cac diem con lai (BR06-06).
     *
     *     km phat sinh = kho -> diem su co + diem su co -> cac diem con lai -> kho
     *
     * Ca chuyen deu la km phat sinh chu khong tru di phan tuyen cu: chiec xe hong da chay het
     * nhung km cua no roi, so met cua xe cuu ho la met chay THEM that su.
     */
    private java.util.Optional<SolutionOption> buildRescueOption(List<RouteStop> remainingStops,
                                                                 MatrixContext context) {
        double neededWeight = 0;
        double neededVolume = 0;
        for (RouteStop stop : remainingStops) {
            neededWeight += stop.getOrder().getTotalWeight();
            neededVolume += stop.getOrder().getTotalVolume();
        }

        /*
         * Chon chiec NHO NHAT ma van du cho — khong dieu xe 1.5 tan di giao ba thung hang.
         * Cac xe khac deu cho ra cung mot con so km vi lo trinh giong het nhau, nen hien nhieu
         * dong giong nhau chi lam dieu phoi vien mat thoi gian doc.
         */
        Vehicle rescue = null;
        for (Vehicle vehicle : vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)) {
            if (vehicle.getMaxWeight() < neededWeight || vehicle.getMaxVolume() < neededVolume) {
                continue;   // BR06-09: khong du suc chua thi loai khoi danh sach
            }
            if (rescue == null || vehicle.getMaxWeight() < rescue.getMaxWeight()) {
                rescue = vehicle;
            }
        }
        if (rescue == null) {
            return java.util.Optional.empty();
        }

        double[][] matrix = context.matrix();
        double extra = matrix[DEPOT_INDEX][INCIDENT_INDEX];

        int previous = INCIDENT_INDEX;
        for (RouteStop stop : remainingStops) {
            int current = context.indexOfOrder(stop.getOrder().getId());
            extra += matrix[previous][current];
            previous = current;
        }
        extra += matrix[previous][DEPOT_INDEX];   // chang cuoi ve kho (CBR-01)

        /*
         * Gio xuat phat cua cac diem con lai = BAY GIO + thoi gian xe cuu ho chay tu kho toi
         * diem su co. Sang hang xong xe moi bat dau chang dau tien, nen moc tinh ETA la luc no
         * co mat tai diem su co chu khong phai luc no roi kho.
         */
        double toIncidentMinutes = (matrix[DEPOT_INDEX][INCIDENT_INDEX] / 1000.0) / context.speedKmh() * 60.0;
        LocalDateTime arriveAtIncident = LocalDateTime.now().plusMinutes(Math.round(toIncidentMinutes));

        List<StopEta> etas = estimateEtas(remainingStops, context, INCIDENT_INDEX, arriveAtIncident);

        return java.util.Optional.of(new SolutionOption(
                SolutionType.A_RESCUE_VEHICLE,
                "Điều xe " + rescue.getLicensePlate() + " ra sang hàng",
                extra,
                String.format(VIETNAMESE, "Xe %s (%,.0f kg / %,.1f m³) và một tài xế rảnh",
                        rescue.getLicensePlate(), rescue.getMaxWeight(), rescue.getMaxVolume()),
                rescue.getId(),
                null,
                etas));
    }

    // ==================== CN-06-08 phuong an B ====================

    /**
     * Chen cac diem con lai vao mot tuyen khac dang chay, dung Cheapest Insertion (BR06-07).
     *
     * PHAI CHEN DUOC HET thi phuong an moi kha thi. Chen duoc ba trong bon diem khong giup gi:
     * diem con lai van khong ai giao, ma dieu phoi vien lai tuong da xong.
     *
     * Suc chua con lai cua tuyen ung vien tinh theo CAC DIEM CHUA GIAO chu khong theo toan bo
     * tuyen: hang cua nhung diem da giao xong thi khong con tren xe nua.
     */
    private List<SolutionOption> buildInsertionOptions(List<RouteStop> remainingStops,
                                                       List<Route> candidateRoutes,
                                                       MatrixContext context) {
        List<SolutionOption> options = new ArrayList<>();
        CheapestInsertion cheapestInsertion = new CheapestInsertion();

        for (Route candidate : candidateRoutes) {
            List<RouteStop> candidateRemaining = remainingStopsOf(candidate);
            if (candidateRemaining.isEmpty()) {
                continue;   // tuyen do cung sap xong, chen vao khong con y nghia
            }

            OptimizationRoute working = toOptimizationRoute(candidate, candidateRemaining, context);
            double before = RouteDistanceCalculator.calculateTotalDistance(working, context.matrix());

            boolean allInserted = true;
            for (RouteStop stop : remainingStops) {
                OptimizationPoint point = context.pointOfOrder(stop.getOrder().getId());
                // lastVisitedIndex = -1: danh sach nay da bat dau tu diem ke tiep cua xe roi
                if (!cheapestInsertion.insertCheapest(working, point, -1, context.matrix())) {
                    allInserted = false;
                    break;   // BR06-09: khong du suc chua thi loai ca phuong an
                }
            }
            if (!allInserted) {
                continue;
            }

            double after = RouteDistanceCalculator.calculateTotalDistance(working, context.matrix());

            /*
             * Moc tinh ETA cua tuyen ung vien la VI TRI HIEN TAI cua no, tuc diem no vua giao
             * xong; chua giao diem nao thi lay kho. Lay kho trong moi truong hop la sai theo
             * huong lac quan: xe da di duoc mot doan roi ma van tinh nhu vua roi kho.
             */
            int originIndex = currentPositionIndexOf(candidate, context);
            List<StopEta> etas = estimateEtasOfRoute(working, context, originIndex, LocalDateTime.now());

            String driverName = (candidate.getDriver() == null) ? "chưa có tài xế"
                    : candidate.getDriver().getFullName();

            options.add(new SolutionOption(
                    SolutionType.B_INSERT_OTHER_ROUTE,
                    "Chèn vào tuyến " + candidate.getCode() + " — " + candidate.getVehicle().getLicensePlate(),
                    after - before,
                    String.format("Tuyến %s của %s nhận thêm %d điểm, không cần huy động xe mới",
                            candidate.getCode(), driverName, remainingStops.size()),
                    null,
                    candidate.getId(),
                    etas));
        }
        return options;
    }

    // ==================== CN-06-09 phuong an C ====================

    /** Hoan sang hom sau. Km phat sinh bang 0 nhung luon xep cuoi (BR06-08, BR06-11). */
    private SolutionOption buildPostponeOption(List<RouteStop> remainingStops) {
        List<StopEta> etas = new ArrayList<>();
        for (RouteStop stop : remainingStops) {
            Order order = stop.getOrder();
            // Hoan thi chua co gio cu the, hen sang hom sau roi phan tuyen lai moi biet
            etas.add(new StopEta(order.getCustomer().getName(), order.getCode(), null));
        }

        return new SolutionOption(
                SolutionType.C_POSTPONE,
                "Hoãn " + remainingStops.size() + " điểm còn lại sang ngày mai",
                0,
                "Không cần huy động gì, nhưng khách không nhận được hàng hôm nay",
                null,
                null,
                etas);
    }

    // ==================== Ma tran dung chung ====================

    /**
     * Ma tran khoang cach va cac chi so tra nhanh, dung chung cho ca ba phuong an.
     *
     * @param indexByOrderId chi so trong ma tran cua tung don
     * @param pointByOrderId doi tuong diem tuong ung, de dua thang cho CheapestInsertion
     */
    private record MatrixContext(double[][] matrix,
                                 Map<Long, Integer> indexByOrderId,
                                 Map<Long, OptimizationPoint> pointByOrderId,
                                 double speedKmh,
                                 int loadingMinutes) {

        int indexOfOrder(Long orderId) {
            return indexByOrderId.get(orderId);
        }

        OptimizationPoint pointOfOrder(Long orderId) {
            return pointByOrderId.get(orderId);
        }
    }

    private MatrixContext buildMatrix(Incident incident, Route brokenRoute,
                                      List<RouteStop> remainingStops, List<Route> candidateRoutes) {

        List<OptimizationPoint> points = new ArrayList<>();

        // Chi so 0 la kho, chi so 1 la diem xay ra su co — quy uoc rieng cua bai toan nay
        points.add(OptimizationPoint.depot(
                brokenRoute.getDepot().getLatitude(), brokenRoute.getDepot().getLongitude()));
        points.add(OptimizationPoint.forOrder(null, "Vi tri su co",
                incident.getLatitude(), incident.getLongitude(), 0, 0));

        Map<Long, OptimizationPoint> pointByOrderId = new LinkedHashMap<>();
        addStops(points, pointByOrderId, remainingStops);
        for (Route candidate : candidateRoutes) {
            addStops(points, pointByOrderId, remainingStopsOf(candidate));
        }

        // loadMatrix dat luon matrixIndex cho tung diem theo dung thu tu danh sach
        DistanceMatrixService.DistanceMatrix matrix = distanceMatrixService.loadMatrix(points);

        Map<Long, Integer> indexByOrderId = new LinkedHashMap<>();
        for (Map.Entry<Long, OptimizationPoint> entry : pointByOrderId.entrySet()) {
            indexByOrderId.put(entry.getKey(), entry.getValue().getMatrixIndex());
        }

        return new MatrixContext(matrix.meters(), indexByOrderId, pointByOrderId,
                systemParameterService.getDouble(SystemParameterService.AVERAGE_SPEED),
                systemParameterService.getInt(SystemParameterService.LOADING_TIME));
    }

    private void addStops(List<OptimizationPoint> points,
                          Map<Long, OptimizationPoint> pointByOrderId,
                          List<RouteStop> stops) {
        for (RouteStop stop : stops) {
            Order order = stop.getOrder();
            if (pointByOrderId.containsKey(order.getId())) {
                continue;   // don da co trong ma tran roi, khong them lan hai
            }
            OptimizationPoint point = OptimizationPoint.forOrder(
                    order.getId(), order.getCustomer().getName(),
                    order.getLatitude(), order.getLongitude(),
                    order.getTotalWeight(), order.getTotalVolume());
            pointByOrderId.put(order.getId(), point);
            points.add(point);
        }
    }

    // ==================== Ham dung chung ====================

    /**
     * Cac tuyen co the nhan them diem: cung ngay, dang chay, khac tuyen bi su co.
     *
     * Chi lay tuyen IN_PROGRESS chu khong lay CONFIRMED: tuyen chua xuat phat thi xe con o kho,
     * ma o kho thi dung phuong an A (dieu han mot xe khac) gon hon nhieu.
     */
    private List<Route> candidateRoutesFor(Route brokenRoute) {
        List<Route> candidates = new ArrayList<>();
        for (Route route : routeRepository.findByDeliveryDate(brokenRoute.getDeliveryDate())) {
            if (!route.getId().equals(brokenRoute.getId())
                    && route.getStatus() == RouteStatus.IN_PROGRESS) {
                candidates.add(route);
            }
        }
        return candidates;
    }

    private List<RouteStop> remainingStopsOf(Route route) {
        List<RouteStop> remaining = new ArrayList<>();
        for (RouteStop stop : route.getStops()) {
            if (stop.getStatus() == StopStatus.PENDING || stop.getStatus() == StopStatus.DELIVERING) {
                remaining.add(stop);
            }
        }
        return remaining;
    }

    /** Dung doi tuong tuyen cua thuat toan tu cac diem CHUA giao cua mot tuyen dang chay. */
    private OptimizationRoute toOptimizationRoute(Route route, List<RouteStop> remainingStops,
                                                  MatrixContext context) {
        Vehicle vehicle = route.getVehicle();
        OptimizationRoute working = new OptimizationRoute(new OptimizationVehicle(
                vehicle.getId(), vehicle.getLicensePlate(),
                vehicle.getMaxWeight(), vehicle.getMaxVolume()));

        for (RouteStop stop : remainingStops) {
            working.addPoint(context.pointOfOrder(stop.getOrder().getId()));
        }
        return working;
    }

    /** Chi so cua diem xe dang dung: diem vua giao xong, chua giao diem nao thi la kho. */
    private int currentPositionIndexOf(Route route, MatrixContext context) {
        int position = DEPOT_INDEX;
        for (RouteStop stop : route.getStops()) {
            if (stop.getStatus() == StopStatus.DELIVERED
                    || stop.getStatus() == StopStatus.FAILED
                    || stop.getStatus() == StopStatus.POSTPONED) {
                position = context.indexOfOrder(stop.getOrder().getId());
            }
        }
        return position;
    }

    /**
     * Gio du kien den tung diem cua mot danh sach diem, tinh tu mot moc xuat phat cho truoc.
     *
     * Dung lai EtaCalculator cua thuat toan chu khong viet cong thuc thu hai — hai cong thuc
     * song song som muon lech nhau, ma luc do gio bao cho khach va gio hien cho tai xe se khac.
     */
    private List<StopEta> estimateEtas(List<RouteStop> stops, MatrixContext context,
                                       int originIndex, LocalDateTime departure) {
        OptimizationRoute working = new OptimizationRoute(null);
        for (RouteStop stop : stops) {
            working.addPoint(context.pointOfOrder(stop.getOrder().getId()));
        }
        return estimateEtasOfRoute(working, context, originIndex, departure);
    }

    private List<StopEta> estimateEtasOfRoute(OptimizationRoute working, MatrixContext context,
                                              int originIndex, LocalDateTime departure) {
        EtaCalculator.assignEta(working, context.matrix(), originIndex, departure,
                context.speedKmh(), context.loadingMinutes());

        List<StopEta> etas = new ArrayList<>();
        for (OptimizationPoint point : working.getPoints()) {
            etas.add(new StopEta(point.getName(), null, point.getEstimatedArrival()));
        }
        return etas;
    }
}
