package com.minhphu.mproute.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhphu.mproute.entity.Depot;
import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.DepotService;
import com.minhphu.mproute.service.RouteGeometryService;
import com.minhphu.mproute.service.RoutePlanService;
import com.minhphu.mproute.service.RoutingService;
import com.minhphu.mproute.service.SystemParameterService;
import com.minhphu.mproute.service.optimization.DistanceMatrixService;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SCR-09 — Man hinh phan tuyen (CN-03-01 -> CN-03-14).
 *
 * Controller nay chi lam ba viec: nhan tham so tu form, goi service, do ket qua ra model.
 * Toan bo nghiep vu phan tuyen nam o RoutingService, con thuat toan nam o
 * service/optimization — day la ranh gioi giup thuat toan van chay duoc bang main().
 *
 * Phan quyen giong man hinh don hang: dieu phoi vien toan quyen, quan tri vien chi xem
 * (docs/01 — ma tran phan quyen).
 */
@Controller
@RequestMapping("/routing")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
@Slf4j
public class RoutingController {

    private static final String NEEDS_MAP = "needsMap";
    private static final String NEEDS_ROUTING_JS = "needsRoutingJs";

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DepotService depotService;
    private final DistanceMatrixService distanceMatrixService;
    private final RoutingService routingService;
    private final RoutePlanService routePlanService;
    private final RouteGeometryService routeGeometryService;
    private final SystemParameterService systemParameterService;
    private final ObjectMapper objectMapper;

    /** Mot ghim tren ban do. Chi chua thu can ve, khong day ca entity ra giao dien. */
    public record MapPoint(String name,
                           double latitude,
                           double longitude,
                           boolean depot,
                           String description) {}

    @GetMapping
    public String screen(@RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                         @RequestParam(required = false) AlgorithmType algorithm,
                         Model model) {

        // Mac dinh xem ngay mai — dung ngay ma don duoc tao san (BR02-06)
        LocalDate targetDate = (deliveryDate != null) ? deliveryDate : LocalDate.now().plusDays(1);

        // BR03-01: chi don DA DUYET moi duoc dua vao phan tuyen
        List<Order> orders = orderRepository.findByDeliveryDateAndStatus(targetDate, OrderStatus.APPROVED);

        double totalWeight = orders.stream().mapToDouble(Order::getTotalWeight).sum();
        double totalVolume = orders.stream().mapToDouble(Order::getTotalVolume).sum();

        model.addAttribute("deliveryDate", targetDate);
        model.addAttribute("orders", orders);
        model.addAttribute("totalWeight", totalWeight);
        model.addAttribute("totalVolume", totalVolume);

        Depot depot = findActiveDepot(model);
        model.addAttribute("depot", depot);

        // CN-03-03: chi xe AVAILABLE moi duoc dua ra cho tich chon (CBR-05, BR03-02)
        List<Vehicle> vehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
        model.addAttribute("vehicles", vehicles);

        // CN-03-04: ba bo giai, MANUAL khong phai thuat toan chay tu dong nen khong liet ke
        model.addAttribute("algorithms",
                List.of(AlgorithmType.OR_TOOLS, AlgorithmType.NN, AlgorithmType.NN_2OPT));
        model.addAttribute("selectedAlgorithm",
                (algorithm != null) ? algorithm : routingService.defaultAlgorithm());

        /*
         * CN-03-07: ket qua lan chay gan nhat lay THANG TU CSDL chu khong giu trong session.
         * Nho vay tai lai trang, dong may roi mo lai, hay mo o tab khac deu thay dung mot
         * ket qua — va do cung chinh la du lieu ma buoc chot ke hoach se dung.
         */
        List<Route> routes = routingService.visibleRoutesOf(targetDate);
        model.addAttribute("routes", routes);
        model.addAttribute("totalRouteDistance",
                routes.stream().mapToDouble(Route::getTotalDistance).sum());

        /*
         * CN-04-06 — o chon tai xe cua tung tuyen. Danh sach tinh RIENG cho tung tuyen chu
         * khong dung chung mot danh sach: nguoi da nhan tuyen khac trong ngay phai bien mat
         * khoi o chon cua cac tuyen con lai (BR04-07), nhung van phai con trong o chon cua
         * chinh tuyen ho dang giu, neu khong thi mo ra thay trong nhu chua gan ai.
         */
        Map<Long, List<Driver>> driverOptions = new LinkedHashMap<>();
        for (Route route : routes) {
            driverOptions.put(route.getId(), routePlanService.selectableDriversFor(targetDate, route));
        }
        model.addAttribute("driverOptions", driverOptions);

        // CN-04-07: chi chot duoc khi con tuyen nhap, moi tuyen co tai xe, va khong sot don nao
        boolean hasDraft = routes.stream().anyMatch(route -> route.getStatus() == RouteStatus.DRAFT);
        boolean everyDraftHasDriver = routes.stream()
                .filter(route -> route.getStatus() == RouteStatus.DRAFT)
                .allMatch(route -> route.getDriver() != null);
        model.addAttribute("canConfirmPlan", hasDraft && everyDraftHasDriver && orders.isEmpty());

        /*
         * CN-03-14: don con o trang thai APPROVED trong khi da co tuyen nhap chinh la don
         * thuat toan khong xep duoc — don duoc xep da chuyen sang ROUTED. Suy ra tu trang
         * thai nhu vay thi tai lai trang van con canh bao, khong phu thuoc flash message.
         */
        model.addAttribute("unassignedOrders", routes.isEmpty() ? List.of() : orders);

        var metrics = routes.isEmpty() ? null : routingService.latestMetricsOf(targetDate);
        model.addAttribute("metrics", metrics);

        /*
         * CN-04-04 — dat hai con so canh nhau: quang duong thuat toan tim ra (luu tai thoi diem
         * chay, khong bao gio tinh lai, CBR-18) va quang duong cua cac tuyen hien tai. Hai con so
         * bang nhau khi chua ai dong vao; lech nhau la do da chinh tay.
         */
        boolean manuallyEdited = routes.stream()
                .anyMatch(route -> route.getAlgorithm() == AlgorithmType.MANUAL);
        model.addAttribute("manuallyEdited", manuallyEdited);
        model.addAttribute("automaticDistance", metrics == null ? null : metrics.getOptimizedDistance());

        // CN-04-01/02: ma tran de trinh duyet hien ngay con so moi khi keo tha, chi la xem truoc
        model.addAttribute("planJson", toJson(buildPlanData(depot, routes), "null"));

        /*
         * CN-03-09 — duong di THAT cua tung tuyen, lay tu OSRM Route API.
         *
         * Ghim va duong di di theo hai khoi JSON khac nhau, va hai khoi nay khong bao gio
         * chong nhau: don da xep tuyen thi trang thai la ROUTED nen khong con trong danh
         * sach "orders" (von chi loc APPROVED). Diem da co tuyen duoc ve bang ghim DANH SO
         * theo mau cua tuyen, diem chua xep duoc ve bang cham xam de nhin ra ngay.
         */
        List<RouteGeometryService.RouteLine> routeLines =
                routeGeometryService.buildRouteLines(depot, routes);
        model.addAttribute("routeLinesJson", toJson(routeLines, "[]"));
        model.addAttribute("mapPointsJson", buildMapPointsJson(depot, orders));

        // Mot tuyen ve bang doan thang la ca ban do dang hien duong khong co that (CBR-11)
        model.addAttribute("approximatePath",
                routeLines.stream().anyMatch(RouteGeometryService.RouteLine::approximate));

        model.addAttribute(NEEDS_MAP, true);
        model.addAttribute(NEEDS_ROUTING_JS, true);
        return "routing/routing";
    }

    /**
     * CN-03-06 — chay thuat toan sinh tuyen cho ngay dang xem.
     *
     * Bat IllegalArgumentException va IllegalStateException de doi loi nghiep vu thanh mot
     * dong canh bao mau do tren chinh man hinh nay. Thieu suc chua (MSG-08) hay chon phai
     * xe dang chay la tinh huong nguoi dung xu ly duoc, khong phai su co he thong — nem ra
     * trang 500 thi dieu phoi vien mat het lua chon vua tich.
     */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/optimize")
    public String optimize(@RequestParam
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                           @RequestParam(required = false) List<Long> vehicleIds,
                           @RequestParam(required = false) AlgorithmType algorithm,
                           @AuthenticationPrincipal CurrentUser currentUser,
                           RedirectAttributes redirectAttributes) {

        try {
            RoutingService.RoutingOutcome outcome =
                    routingService.optimize(deliveryDate, vehicleIds, algorithm, currentUser.getId());

            redirectAttributes.addFlashAttribute("successMessage", summaryOf(outcome));

            if (outcome.hasUnassignedOrders()) {
                // MSG-16 — tong suc chua du ma van khong xep het don (CN-03-14, BR03-13)
                redirectAttributes.addFlashAttribute("errorMessage", String.format(
                        "Còn %d đơn chưa xếp được vào tuyến nào. Vui lòng chọn thêm xe.",
                        outcome.unassignedOrders().size()));
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        String algorithmParam = (algorithm != null) ? "&algorithm=" + algorithm.name() : "";
        return "redirect:/routing?deliveryDate=" + deliveryDate + algorithmParam;
    }

    /**
     * CN-03-05 — nap truoc ma tran khoang cach cho tap diem cua ngay dang xem.
     *
     * Bam lan hai voi cung tap diem thi so o "lay moi" phai bang 0 va khong co dong
     * log nao goi OSRM — day chinh la cach chung minh bo nho dem hoat dong (CBR-12).
     */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/prepare-matrix")
    public String prepareMatrix(@RequestParam
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                RedirectAttributes redirectAttributes) {

        List<Order> orders = orderRepository.findByDeliveryDateAndStatus(deliveryDate, OrderStatus.APPROVED);
        if (orders.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ngày này chưa có đơn nào đã duyệt nên chưa có điểm giao để tính khoảng cách.");
            return "redirect:/routing?deliveryDate=" + deliveryDate;
        }

        Depot depot = depotService.getActiveDepot();
        List<OptimizationPoint> points = buildOptimizationPoints(depot, orders);

        long startedAt = System.currentTimeMillis();
        DistanceMatrixService.DistanceMatrix matrix = distanceMatrixService.loadMatrix(points);
        long elapsedMs = System.currentTimeMillis() - startedAt;

        int size = points.size();
        String message = String.format(
                "Đã chuẩn bị ma trận %d×%d (%d ô) trong %d ms. "
                        + "Lấy từ bộ nhớ đệm: %d ô. Phải lấy mới: %d ô. Nguồn: %s.",
                size, size, size * size - size, elapsedMs,
                matrix.cacheHits(), matrix.cacheMisses(), matrix.source().getDisplayName());

        if (matrix.approximate()) {
            // MSG-09
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không kết nối được OSRM. Đang dùng khoảng cách xấp xỉ (Haversine × hệ số). "
                            + "Tuyến sinh ra từ ma trận này sẽ được đánh dấu là xấp xỉ.");
        }
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    /** CN-04-06 — gan tai xe cho mot tuyen nhap. */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/routes/{routeId}/driver")
    public String assignDriver(@PathVariable Long routeId,
                               @RequestParam Long driverId,
                               @RequestParam
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                               RedirectAttributes redirectAttributes) {

        try {
            var route = routePlanService.assignDriver(routeId, driverId);
            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "Đã giao tuyến %s cho tài xế %s.",
                    route.getCode(), route.getDriver().getFullName()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    /**
     * CN-04-01, CN-04-02 — luu cach sap xep do dieu phoi vien keo tha.
     *
     * Nhan hai danh sach SONG SONG thay vi mot khoi JSON: routeIds[i] di voi orderIdsPerRoute[i].
     * Form thuong gui duoc kieu nay ma khong can JavaScript dung JSON, va khong can them thu
     * vien nao — tuyen khong con don nao thi gui chuoi rong.
     *
     * NGAN CACH BANG DAU GACH NGANG CHU KHONG PHAI DAU PHAY. Spring TU DONG tach chuoi co dau
     * phay khi bind vao List<String>, nen "5,6,7" cua mot tuyen se bien thanh ba phan tu va hai
     * danh sach song song lech nhau ngay. Loi nay khong bao gi ro rang, no chi bao "du lieu
     * khong hop le" — mat mot luc moi truy ra.
     */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/arrange")
    public String arrange(@RequestParam
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                          @RequestParam(required = false) List<Long> routeIds,
                          @RequestParam(required = false) List<String> orderIdsPerRoute,
                          RedirectAttributes redirectAttributes) {

        try {
            routePlanService.applyManualArrangement(deliveryDate, toManualRoutes(routeIds, orderIdsPerRoute));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lưu phương án chỉnh tay. Quãng đường và giờ dự kiến đã được tính lại.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    /** CN-04-05 — bỏ phương án chỉnh tay, chạy lại thuật toán để về kết quả gốc. */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/restore")
    public String restore(@RequestParam
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                          @AuthenticationPrincipal CurrentUser currentUser,
                          RedirectAttributes redirectAttributes) {

        try {
            routePlanService.restoreAutomaticPlan(deliveryDate, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã khôi phục kết quả của thuật toán, bỏ mọi thay đổi chỉnh tay.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    /** CN-04-07 — chot ke hoach cua ca ngay. */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/confirm")
    public String confirmPlan(@RequestParam
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                              @AuthenticationPrincipal CurrentUser currentUser,
                              RedirectAttributes redirectAttributes) {

        try {
            RoutePlanService.ConfirmResult result =
                    routePlanService.confirmPlan(deliveryDate, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "Đã chốt %d tuyến. Xe chuyển sang trạng thái Đang chạy, %d tài xế nhận được thông báo.",
                    result.routeCount(), result.notifiedDriverCount()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    /** CN-04-09 — huy mot tuyen, tra don ve cho phan tuyen va tra xe ve san sang. */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/routes/{routeId}/cancel")
    public String cancelRoute(@PathVariable Long routeId,
                              @RequestParam(required = false) String reason,
                              @RequestParam
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                              @AuthenticationPrincipal CurrentUser currentUser,
                              RedirectAttributes redirectAttributes) {

        try {
            var route = routePlanService.cancelRoute(routeId, reason, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "Đã hủy tuyến %s. Các đơn của tuyến này quay lại danh sách chờ phân tuyến.",
                    route.getCode()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    // ==================== Ham rieng ====================

    /**
     * Doi hai danh sach song song tu form thanh danh sach tuyen da sap.
     *
     * Chuoi rong nghia la tuyen do khong con don nao — hop le, dieu phoi vien co the keo het
     * don cua mot tuyen sang tuyen khac. Bo qua phan tu rong chu khong nem loi.
     */
    private List<RoutePlanService.ManualRoute> toManualRoutes(List<Long> routeIds,
                                                              List<String> orderIdsPerRoute) {
        if (routeIds == null || routeIds.isEmpty()) {
            throw new IllegalArgumentException("Không nhận được phương án sắp xếp nào.");
        }
        if (orderIdsPerRoute == null || orderIdsPerRoute.size() != routeIds.size()) {
            throw new IllegalArgumentException("Dữ liệu sắp xếp gửi lên không hợp lệ. Tải lại trang rồi thử lại.");
        }

        List<RoutePlanService.ManualRoute> manualRoutes = new ArrayList<>();
        for (int i = 0; i < routeIds.size(); i++) {
            List<Long> orderIds = new ArrayList<>();
            for (String part : orderIdsPerRoute.get(i).split("-")) {
                if (!part.isBlank()) {
                    try {
                        orderIds.add(Long.valueOf(part.trim()));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(
                                "Dữ liệu sắp xếp gửi lên không hợp lệ. Tải lại trang rồi thử lại.");
                    }
                }
            }
            manualRoutes.add(new RoutePlanService.ManualRoute(routeIds.get(i), orderIds));
        }
        return manualRoutes;
    }

    /** Mot diem trong khoi du lieu de trinh duyet tinh thu quang duong luc keo tha. */
    public record PlanPoint(Long orderId, int matrixIndex, double weight, double volume) {}

    /**
     * Ma tran khoang cach va tham so ETA, de trinh duyet hien ngay con so moi khi keo tha
     * (CN-04-01, CN-04-02, BR04-03).
     *
     * CHI LA XEM TRUOC. Luc bam Luu thi may chu tinh lai bang dung ham cua thuat toan, va con
     * so cua may chu moi la con so ghi xuong CSDL. Day la ly do khoi nay chi chua ma tran chu
     * khong chua san km cua tung tuyen: trinh duyet tu cong lay, con so cuoi cung van do Java quyet.
     */
    public record PlanData(double[][] matrix,
                           List<PlanPoint> points,
                           double speedKmh,
                           int loadingMinutes,
                           String departureTime) {}

    private PlanData buildPlanData(Depot depot, List<Route> routes) {
        if (depot == null || routes.isEmpty()) {
            return null;
        }

        // Kho luon o chi so 0 — quy uoc xuyen suot do an, JavaScript dua vao dung quy uoc nay
        List<OptimizationPoint> points = new ArrayList<>();
        points.add(OptimizationPoint.depot(depot.getLatitude(), depot.getLongitude()));

        List<Order> ordersInRoutes = new ArrayList<>();
        for (Route route : routes) {
            for (var stop : route.getStops()) {
                Order order = stop.getOrder();
                ordersInRoutes.add(order);
                points.add(OptimizationPoint.forOrder(order.getId(), order.getCustomer().getName(),
                        order.getLatitude(), order.getLongitude(),
                        order.getTotalWeight(), order.getTotalVolume()));
            }
        }
        if (ordersInRoutes.isEmpty()) {
            return null;
        }

        // loadMatrix dat luon matrixIndex cho tung diem theo dung thu tu danh sach o tren
        var matrix = routingService.matrixFor(points);

        List<PlanPoint> planPoints = new ArrayList<>();
        for (int i = 0; i < ordersInRoutes.size(); i++) {
            Order order = ordersInRoutes.get(i);
            planPoints.add(new PlanPoint(order.getId(), points.get(i + 1).getMatrixIndex(),
                    order.getTotalWeight(), order.getTotalVolume()));
        }

        return new PlanData(matrix.meters(), planPoints,
                systemParameterService.getDouble(SystemParameterService.AVERAGE_SPEED),
                systemParameterService.getInt(SystemParameterService.LOADING_TIME),
                routingService.departureTime().toString());
    }

    /**
     * Cau tom tat sau mot lan chay, dat ngay dau man hinh.
     *
     * Co y neu ca thoi gian chay (BR03-10) va muc giam so voi moc A: hai con so nay chinh
     * la thu hoi dong hoi den dau tien, va de o day thi khong phai keo xuong bang chi so
     * moi doc duoc.
     */
    private String summaryOf(RoutingService.RoutingOutcome outcome) {
        var metrics = outcome.metrics();
        return String.format(Locale.ROOT,
                "Đã sinh %d tuyến bằng %s trong %d ms. Tổng %.2f km, giảm %.1f%% so với mốc A.",
                outcome.routes().size(),
                metrics.getAlgorithm().getDisplayName(),
                metrics.getExecutionTimeMs(),
                metrics.getOptimizedDistance() / 1000.0,
                metrics.getReductionAPercent());
    }

    /**
     * Doi don hang sang diem cua bai toan.
     *
     * KHO LUON DUNG DAU danh sach — quy uoc xuyen suot do an: chi so 0 trong ma tran
     * la kho. DistanceMatrixService dua vao dung thu tu nay de danh matrixIndex.
     */
    private List<OptimizationPoint> buildOptimizationPoints(Depot depot, List<Order> orders) {
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

    /**
     * Dung chuoi JSON cac ghim RIENG LE de JavaScript ve len ban do: kho, va cac don chua
     * duoc xep vao tuyen nao. Diem da co tuyen di theo routeLinesJson chu khong qua day.
     */
    private String buildMapPointsJson(Depot depot, List<Order> orders) {
        List<MapPoint> mapPoints = new ArrayList<>();

        if (depot != null) {
            mapPoints.add(new MapPoint(depot.getName(), depot.getLatitude(), depot.getLongitude(),
                    true, depot.getAddress()));
        }

        for (Order order : orders) {
            int boxCount = order.getItems().stream().mapToInt(item -> item.getBoxCount()).sum();
            // Locale.ROOT de dau thap phan luon la dau cham, dung quy uoc hien thi o docs/05
            String description = String.format(Locale.ROOT, "%d thùng · %.1f kg · %.3f m³",
                    boxCount, order.getTotalWeight(), order.getTotalVolume());
            mapPoints.add(new MapPoint(order.getCustomer().getName(),
                    order.getLatitude(), order.getLongitude(), false, description));
        }

        return toJson(mapPoints, "[]");
    }

    /**
     * Doi mot doi tuong sang JSON cho JavaScript doc.
     *
     * Dung Jackson chu khong tu noi chuoi: ten khach hang co the chua dau nhay kep
     * ("Tap hoa "Ba Lan""), noi tay thi vo JSON va ca ban do trang.
     */
    private String toJson(Object value, String fallback) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            // Khong de mot loi dinh dang lam hong ca man hinh: ban do rong, bang van con
            log.error("Khong dung duoc JSON cho ban do phan tuyen", ex);
            return fallback;
        }
    }

    /** Lay kho dang hoat dong. Chua khai bao kho thi bao ro chu khong nem loi 500. */
    private Depot findActiveDepot(Model model) {
        try {
            return depotService.getActiveDepot();
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage",
                    "Chưa có kho nào đang hoạt động. Vào Danh mục → Kho để khai báo trước khi phân tuyến.");
            return null;
        }
    }
}
