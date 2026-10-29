package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Notification;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.NotificationSendStatus;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.DriverRepository;
import com.minhphu.mproute.repository.NotificationRepository;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bien ket qua thuat toan thanh KE HOACH GIAO HANG THAT: gan tai xe, chot ke hoach, huy tuyen
 * (CN-04-06, CN-04-07, CN-04-09).
 *
 * DAY LA CHO DUY NHAT MOT TUYEN DOI TU "NHAP" SANG "THAT". Truoc khi chot, tuyen chi la mot
 * de xuat: chay lai thuat toan la xoa sach, xe van ranh, tai xe khong thay gi. Sau khi chot,
 * xe bi khoa, tai xe nhin thay tuyen tren dien thoai, va khach se nhan email bao gio giao.
 * Vi vay moi phep kiem cua lop nay deu phai lam o TANG SERVICE chu khong chi an nut tren
 * giao dien — hoi dong se gui thang id tuyen len de thu (CBR-15).
 *
 * Tach khoi RoutingService co chu dich: ben kia lo viec CHAY thuat toan, ben nay lo viec
 * DUYET ket qua. Hai viec doi lap nhau ve trang thai — mot ben xoa tuyen nhap de sinh lai,
 * mot ben khoa tuyen lai khong cho sinh nua.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutePlanService {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /*
     * Tuyen o hai trang thai nay khong con giu tai xe (CBR-06): huy thi chua tung chay, xong thi
     * tai xe da ve kho va nhan duoc chuyen tiep theo trong ngay — xem docs/09 muc B7.
     */
    private static final Set<RouteStatus> STATUSES_RELEASING_DRIVER =
            Set.of(RouteStatus.CANCELLED, RouteStatus.COMPLETED);

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;
    private final StatusHistoryService statusHistoryService;
    private final RoutingService routingService;
    private final VehicleService vehicleService;
    private final CustomerNotificationService customerNotificationService;

    // ==================== Doc ====================

    /**
     * Tai xe con chon duoc cho mot tuyen trong ngay (BR04-07, CBR-06).
     *
     * Loai san nhung nguoi dang co tuyen CHUA XONG khac cung ngay ra khoi o chon, chu khong de
     * nguoi dung chon xong roi moi bao loi. Tai xe cua CHINH tuyen dang xet thi giu lai, neu
     * khong thi mo o chon ra la thay trong, tuong nhu chua gan ai.
     */
    @Transactional(readOnly = true)
    public List<Driver> selectableDriversFor(LocalDate deliveryDate, Route route) {
        Set<Long> takenDriverIds = new HashSet<>();
        for (Route other : routeRepository.findByDeliveryDate(deliveryDate)) {
            if (STATUSES_RELEASING_DRIVER.contains(other.getStatus()) || other.getDriver() == null) {
                continue;
            }
            if (route == null || !other.getId().equals(route.getId())) {
                takenDriverIds.add(other.getDriver().getId());
            }
        }

        List<Driver> selectable = new ArrayList<>();
        for (Driver driver : driverRepository.findByStatus(RecordStatus.ACTIVE)) {
            if (!takenDriverIds.contains(driver.getId())) {
                selectable.add(driver);
            }
        }
        return selectable;
    }

    // ==================== CN-04-06 gan tai xe ====================

    /**
     * Gan tai xe cho mot tuyen nhap.
     *
     * Gan tai xe KHONG phai mot lan chuyen trang thai nen khong ghi status_history: tuyen van
     * la DRAFT truoc va sau. Dong lich su chi ghi luc chot (CBR-14).
     */
    @Transactional
    public Route assignDriver(Long routeId, Long driverId) {
        Route route = requireRoute(routeId);

        // CBR-10: tuyen da chot khong sua duoc, doi tai xe phai huy tuyen roi chot lai
        if (route.getStatus() != RouteStatus.DRAFT) {
            throw new IllegalStateException(String.format(
                    "Tuyến %s đang ở trạng thái \"%s\" nên không đổi tài xế được. "
                            + "Hủy tuyến rồi chốt lại nếu cần đổi người.",
                    route.getCode(), route.getStatus().getDisplayName()));
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài xế được chọn."));

        if (driver.getStatus() != RecordStatus.ACTIVE) {
            throw new IllegalStateException(String.format(
                    "Tài xế %s đã ngừng làm việc nên không giao tuyến được.", driver.getFullName()));
        }

        /*
         * CBR-06 — mot tai xe khong cam hai tuyen chua xong trong cung mot ngay. Kiem lai o day
         * chu khong tin vao o chon: gui thang driverId cua nguoi dang co tuyen len thi van phai
         * bi tu choi.
         */
        for (Route other : routeRepository.findByDeliveryDate(route.getDeliveryDate())) {
            if (other.getId().equals(route.getId()) || STATUSES_RELEASING_DRIVER.contains(other.getStatus())) {
                continue;
            }
            if (other.getDriver() != null && other.getDriver().getId().equals(driverId)) {
                throw new IllegalStateException(String.format(
                        "Tài xế %s đang có tuyến %s chưa xong trong ngày này.",
                        driver.getFullName(), other.getCode()));
            }
        }

        route.setDriver(driver);
        log.info("Tuyen {} giao cho tai xe {} ({})", route.getCode(), driver.getFullName(), driverId);
        return routeRepository.save(route);
    }

    // ==================== CN-04-01, CN-04-02, CN-04-03 chinh tay ====================

    /** Mot tuyen sau khi dieu phoi vien keo tha: dung tuyen do, thu tu don moi. */
    public record ManualRoute(Long routeId, List<Long> orderIds) {}

    /**
     * Ghi lai cach sap xep do dieu phoi vien keo tha (CN-04-01, CN-04-02).
     *
     * TRINH DUYET CHI GUI LEN THU TU DON, khong gui km, khong gui ETA, khong gui kg. Con so
     * hien luc keo tha chi la xem truoc cho de nhin; moi con so ghi xuong CSDL deu do may chu
     * tinh lai bang dung ham cua thuat toan (RoutingService.recalculate). Tin con so cua trinh
     * duyet la mo duong cho bat ky ai sua JavaScript cung ghi duoc mot tuyen 0 km vao bao cao.
     *
     * Ba phep kiem, tat ca deu o tang service (CBR-15):
     *   1. moi tuyen phai con la DRAFT — tuyen da chot khong sua duoc (CBR-10)
     *   2. tap don sau khi sap phai TRUNG KHIT tap don truoc do: khong duoc mat don, khong
     *      duoc nhan doi don. Keo tha lam mat mot don thi khong ai phat hien ra cho toi khi
     *      khach goi den hoi
     *   3. moi tuyen phai nam trong suc chua CA VE KG LAN VE M3 (CBR-03, CBR-04, MSG-14)
     */
    @Transactional
    public void applyManualArrangement(LocalDate deliveryDate, List<ManualRoute> arrangement) {
        List<Route> routes =
                routeRepository.findByDeliveryDateAndStatus(deliveryDate, RouteStatus.DRAFT);
        if (routes.isEmpty()) {
            throw new IllegalStateException("Ngày này không còn tuyến nháp nào để chỉnh.");
        }

        Map<Long, Route> routeById = new LinkedHashMap<>();
        for (Route route : routes) {
            routeById.put(route.getId(), route);
        }

        // ---------- 1. chi duoc dung toi tuyen nhap cua chinh ngay do ----------
        for (ManualRoute manualRoute : arrangement) {
            if (!routeById.containsKey(manualRoute.routeId())) {
                throw new IllegalArgumentException(
                        "Có tuyến trong yêu cầu không thuộc kế hoạch nháp của ngày này.");
            }
        }
        if (arrangement.size() != routes.size()) {
            throw new IllegalArgumentException(
                    "Số tuyến gửi lên không khớp với kế hoạch hiện có. Tải lại trang rồi chỉnh lại.");
        }

        // ---------- 2. tap don phai trung khit ----------
        Map<Long, Order> ordersBefore = new LinkedHashMap<>();
        for (Route route : routes) {
            for (RouteStop stop : route.getStops()) {
                ordersBefore.put(stop.getOrder().getId(), stop.getOrder());
            }
        }

        Set<Long> orderIdsAfter = new HashSet<>();
        int totalAfter = 0;
        for (ManualRoute manualRoute : arrangement) {
            for (Long orderId : manualRoute.orderIds()) {
                orderIdsAfter.add(orderId);
                totalAfter++;
            }
        }
        if (totalAfter != orderIdsAfter.size()) {
            throw new IllegalArgumentException("Có đơn bị lặp lại ở hai tuyến. Tải lại trang rồi chỉnh lại.");
        }
        if (!orderIdsAfter.equals(ordersBefore.keySet())) {
            throw new IllegalArgumentException(
                    "Danh sách đơn sau khi chỉnh không khớp với danh sách đơn đang có trên tuyến. "
                            + "Tải lại trang rồi chỉnh lại.");
        }

        // ---------- 3. suc chua tung tuyen, kiem ca hai chieu ----------
        for (ManualRoute manualRoute : arrangement) {
            Route route = routeById.get(manualRoute.routeId());
            double weight = 0;
            double volume = 0;
            for (Long orderId : manualRoute.orderIds()) {
                Order order = ordersBefore.get(orderId);
                weight += order.getTotalWeight();
                volume += order.getTotalVolume();
            }

            Vehicle vehicle = route.getVehicle();
            if (weight > vehicle.getMaxWeight()) {
                // MSG-14
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "Tuyến %s vượt quá tải trọng cho phép của xe %s: %.1f kg so với %.0f kg.",
                        route.getCode(), vehicle.getLicensePlate(), weight, vehicle.getMaxWeight()));
            }
            if (volume > vehicle.getMaxVolume()) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "Tuyến %s vượt quá thể tích cho phép của xe %s: %.3f m³ so với %.1f m³.",
                        route.getCode(), vehicle.getLicensePlate(), volume, vehicle.getMaxVolume()));
            }
        }

        /*
         * Xoa het diem dung cu roi flush TRUOC khi tao diem moi.
         *
         * Bat buoc phai flush o giua: cot route_stops.order_id dat UNIQUE de mot don chi thuoc
         * mot tuyen (CBR-02), va cap (route_id, sequence) cung UNIQUE. Chuyen mot don sang tuyen
         * khac ma khong xoa dong cu truoc thi dong moi dam thang vao hai rang buoc do.
         */
        for (Route route : routes) {
            route.getStops().clear();
        }
        routeRepository.saveAll(routes);
        routeRepository.flush();

        for (ManualRoute manualRoute : arrangement) {
            Route route = routeById.get(manualRoute.routeId());
            int sequence = 1;
            for (Long orderId : manualRoute.orderIds()) {
                RouteStop stop = new RouteStop();
                stop.setOrder(ordersBefore.get(orderId));
                stop.setSequence(sequence++);
                stop.setLegDistance(0.0);      // may chu tinh lai ngay o buoc duoi
                route.addStop(stop);
            }

            /*
             * Danh dau tuyen la CHINH TAY. Bang chi so cua lan chay tu dong van giu nguyen
             * (CBR-18), nho vay man hinh dat duoc hai con so canh nhau: quang duong thuat toan
             * tim ra va quang duong sau khi nguoi sua (CN-04-04).
             */
            route.setAlgorithm(AlgorithmType.MANUAL);
        }

        routeRepository.saveAll(routes);
        routingService.recalculate(routes);

        log.info("Ap dung chinh tay ngay {}: {} tuyen, {} don", deliveryDate, routes.size(), totalAfter);
    }

    /**
     * CN-04-05 — bo phuong an chinh tay, quay ve ket qua cua thuat toan.
     *
     * Cach lam: CHAY LAI dung bo giai da sinh ra ke hoach nay, tren dung doi xe dang giu tuyen.
     * Hai bo giai deu chay tu dinh, khong co yeu to ngau nhien, nen chay lai ra dung ket qua cu.
     *
     * Vi sao khong luu san thu tu goc vao mot cot rieng: bang routes theo docs/02 khong co cho
     * cho no, ma them cot chi de phuc vu nut nay thi khong dang. Chay lai vua don gian vua co
     * loi the la tinh lai luon bang chi so, khong so hai con so lech nhau.
     */
    @Transactional
    public void restoreAutomaticPlan(LocalDate deliveryDate, Long currentUserId) {
        List<Route> routes =
                routeRepository.findByDeliveryDateAndStatus(deliveryDate, RouteStatus.DRAFT);
        if (routes.isEmpty()) {
            throw new IllegalStateException("Ngày này không còn tuyến nháp nào để khôi phục.");
        }

        List<Long> vehicleIds = new ArrayList<>();
        for (Route route : routes) {
            vehicleIds.add(route.getVehicle().getId());
        }

        /*
         * Thuat toan lay tu bang chi so da luu chu khong lay tu route.algorithm: sau khi chinh
         * tay thi route.algorithm da bi doi thanh MANUAL, ma MANUAL khong phai mot bo giai.
         */
        AlgorithmType algorithm = AlgorithmType.MANUAL;
        var metrics = routingService.latestMetricsOf(deliveryDate);
        if (metrics != null && metrics.getAlgorithm() != AlgorithmType.MANUAL) {
            algorithm = metrics.getAlgorithm();
        }
        if (algorithm == AlgorithmType.MANUAL) {
            algorithm = routingService.defaultAlgorithm();
        }

        routingService.optimize(deliveryDate, vehicleIds, algorithm, currentUserId);
        log.info("Khoi phuc ket qua tu dong ngay {} bang {}", deliveryDate, algorithm);
    }

    // ==================== CN-04-07 chot ke hoach ====================

    /** So tuyen da chot va so thong bao da gui, de bao lai cho dieu phoi vien. */
    public record ConfirmResult(int routeCount,
                                int notifiedDriverCount,
                                int mailedCustomerCount,
                                int customersWithoutMailCount) {}

    /**
     * Chot toan bo ke hoach cua mot ngay (CN-04-07, BR04-08).
     *
     * Chot CA NGAY MOT LAN chu khong tung tuyen mot: doi xe cua Minh Phu chi co 4 chiec, chot
     * le te thi de xay ra canh mot nua so tuyen da khoa xe con mot nua chua, ma luc do chay lai
     * thuat toan cung khong duoc ma sua tay cung khong xong.
     *
     * Ba phep kiem truoc khi chot, tat ca deu o tang service:
     *   1. phai co it nhat mot tuyen nhap
     *   2. moi tuyen phai co tai xe (CN-04-06)
     *   3. khong con don nao chua xep duoc (CN-03-14) — chot ma bo quen don thi den chieu
     *      khach goi den hoi moi biet
     */
    @Transactional
    public ConfirmResult confirmPlan(LocalDate deliveryDate, Long currentUserId) {
        List<Route> draftRoutes =
                routeRepository.findByDeliveryDateAndStatus(deliveryDate, RouteStatus.DRAFT);

        if (draftRoutes.isEmpty()) {
            throw new IllegalStateException("Ngày này chưa có tuyến nháp nào để chốt.");
        }

        List<String> routesWithoutDriver = new ArrayList<>();
        for (Route route : draftRoutes) {
            if (route.getDriver() == null) {
                routesWithoutDriver.add(route.getCode());
            }
        }
        if (!routesWithoutDriver.isEmpty()) {
            throw new IllegalStateException(
                    "Chưa gán tài xế cho tuyến " + String.join(", ", routesWithoutDriver)
                            + ". Mỗi tuyến phải có một tài xế trước khi chốt.");
        }

        /*
         * Xe co the vao bao tri SAU luc chay toi uu — vd. hong tren tuyen hom nay (BR06-14) trong
         * khi tuyen nhap ngay mai da xep cho no. Chot ke hoach voi xe hong la hua giao hang bang
         * mot chiec xe khong chay duoc.
         */
        List<String> routesWithBrokenVehicle = new ArrayList<>();
        for (Route route : draftRoutes) {
            if (route.getVehicle().getStatus() == VehicleStatus.MAINTENANCE) {
                routesWithBrokenVehicle.add(route.getCode() + " (" + route.getVehicle().getLicensePlate() + ")");
            }
        }
        if (!routesWithBrokenVehicle.isEmpty()) {
            throw new IllegalStateException(
                    "Xe của tuyến " + String.join(", ", routesWithBrokenVehicle)
                            + " đang bảo trì. Bỏ chọn xe đó rồi chạy lại phân tuyến trước khi chốt.");
        }

        List<Order> stillUnassigned =
                orderRepository.findByDeliveryDateAndStatus(deliveryDate, OrderStatus.APPROVED);
        if (!stillUnassigned.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Còn %d đơn chưa xếp được vào tuyến nào. Chọn thêm xe và chạy lại trước khi chốt.",
                    stillUnassigned.size()));
        }

        int notified = 0;
        int mailed = 0;
        int withoutMail = 0;
        int mailErrors = 0;
        for (Route route : draftRoutes) {
            route.setStatus(RouteStatus.CONFIRMED);
            routeRepository.save(route);
            statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                    RouteStatus.DRAFT.name(), RouteStatus.CONFIRMED.name(),
                    currentUserId, "Chot ke hoach giao ngay " + deliveryDate);

            /*
             * KHONG doi trang thai xe o day. Tuyen CONFIRMED cua ngay D tu no da giu xe cho
             * ngay D (VehicleService.findSchedulableOn), con xe chi thanh "Dang chay" khi tai xe
             * bam nhan hang. Truoc day khoa xe ngay luc chot nen chot ngay 29 vao ngay 28 la
             * ca doi xe bi khoa tu ngay 28 — xem docs/09 muc B6.
             */

            if (notifyDriver(route, "NTF-01", String.format(
                    "Tuyến %s ngày %s đã được chốt. Bạn có %d điểm giao.",
                    route.getCode(), deliveryDate.format(DAY_MONTH_YEAR), route.getStops().size()))) {
                notified++;
            }

            /*
             * CN-04-08, BR04-11 — bao gio giao du kien cho khach ngay khi chot.
             *
             * Goi trong CUNG giao dich chot tuyen: lan chay hong thi cac dong thong bao cung
             * hoan tac theo. Bao cho khach mot lich giao ma tuyen lai khong ton tai la tinh
             * huong te nhat, vi khach se cho ca ngay.
             */
            CustomerNotificationService.MailResult mail =
                    customerNotificationService.notifyRouteConfirmed(route);
            mailed += mail.sentCount();
            withoutMail += mail.missingEmailCount();
            mailErrors += mail.errorCount();
        }

        log.info("Chot ke hoach ngay {}: {} tuyen sang CONFIRMED, {} tai xe nhan thong bao, "
                        + "{} thu da gui, {} khach khong nhan duoc thu, {} thu loi",
                deliveryDate, draftRoutes.size(), notified, mailed, withoutMail, mailErrors);
        return new ConfirmResult(draftRoutes.size(), notified, mailed, withoutMail + mailErrors);
    }

    // ==================== CN-04-09 huy tuyen ====================

    /**
     * Huy mot tuyen (CN-04-09, BR04-09, BR04-10).
     *
     * Tra don ve APPROVED — khong tra thi cac don do treo o ROUTED ma khong tuyen nao chua,
     * khong bao gio duoc phan tuyen lai. Huy xong thi xe tu dong ranh lai cho ngay do vi khong
     * con tuyen CONFIRMED nao giu no (VehicleService.findSchedulableOn).
     */
    @Transactional
    public Route cancelRoute(Long routeId, String reason, Long currentUserId) {
        Route route = requireRoute(routeId);

        // BR04-10 + MSG-10: xe da lan banh thi phai di theo luong su co, khong huy ngang duoc
        if (route.getStatus() == RouteStatus.IN_PROGRESS || route.getStatus() == RouteStatus.INCIDENT) {
            throw new IllegalStateException("Không hủy được tuyến đang chạy.");
        }
        if (route.getStatus() == RouteStatus.CANCELLED || route.getStatus() == RouteStatus.COMPLETED) {
            throw new IllegalStateException(String.format(
                    "Tuyến %s đang ở trạng thái \"%s\" nên không hủy được.",
                    route.getCode(), route.getStatus().getDisplayName()));
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Hủy tuyến bắt buộc nhập lý do.");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Lý do hủy không được quá 500 ký tự.");
        }

        RouteStatus oldStatus = route.getStatus();

        for (RouteStop stop : route.getStops()) {
            Order order = stop.getOrder();
            if (order.getStatus() == OrderStatus.ROUTED) {
                order.setStatus(OrderStatus.APPROVED);
                orderRepository.save(order);
                statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                        OrderStatus.ROUTED.name(), OrderStatus.APPROVED.name(),
                        currentUserId, "Huy tuyen " + route.getCode());
            }
        }

        // Xe dang ngoai duong voi chinh tuyen nay thi ve kho; dang chay tuyen khac thi giu nguyen
        vehicleService.releaseIfIdle(route);

        /*
         * XOA CAC DIEM DUNG CUA TUYEN BI HUY — bat buoc, khong phai don dep cho gon.
         *
         * Cot route_stops.order_id dat UNIQUE tren TOAN BANG de bao dam mot don chi thuoc mot
         * tuyen (CBR-02). Neu de lai cac dong cua tuyen da huy thi don vua duoc tra ve APPROVED
         * se KHONG BAO GIO phan tuyen lai duoc: lan chay sau cham vao dung rang buoc do va chet
         * voi "Duplicate entry ... for key route_stops".
         *
         * Y cua CBR-02 la mot don nam tren toi da mot tuyen DANG SONG. Tuyen da huy thi khong
         * con giu don nua — chinh vong lap ben tren vua tra don ve hang cho — nen giu lai dong
         * diem dung la tu mau thuan voi chinh minh.
         *
         * Khong mat dau vet: lan huy da duoc ghi vao status_history kem ly do, va do cung la
         * nguon du lieu cua trang tra cuu (BR07-06). Giu lai diem dung con sinh loi khac —
         * RouteStopRepository.findByOrderId() se tra ve ETA cua mot tuyen da huy.
         */
        route.getStops().clear();

        route.setStatus(RouteStatus.CANCELLED);
        routeRepository.save(route);
        routeRepository.flush();
        statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                oldStatus.name(), RouteStatus.CANCELLED.name(), currentUserId, reason);

        // NTF-05 — bao cho tai xe da duoc giao tuyen, neu co
        notifyDriver(route, "NTF-05", String.format(
                "Tuyến %s ngày %s đã bị hủy. Lý do: %s",
                route.getCode(), route.getDeliveryDate().format(DAY_MONTH_YEAR), reason));

        log.info("Huy tuyen {} tu trang thai {}: {}", route.getCode(), oldStatus, reason);
        return route;
    }

    // ==================== Ham rieng ====================

    /**
     * Ghi thong bao trong he thong cho tai xe cua tuyen. Tra ve false khi tuyen chua co tai xe
     * hoac tai xe khong co tai khoan dang nhap.
     *
     * sendStatus dat SENT chu khong phai NOT_SENT: day la thong bao TRONG he thong, ghi duoc ban
     * ghi la tai xe mo dien thoai ra thay ngay. NOT_SENT danh cho email khong gui duoc (BR04-12).
     */
    private boolean notifyDriver(Route route, String eventCode, String content) {
        Driver driver = route.getDriver();
        if (driver == null || driver.getUser() == null) {
            return false;
        }

        Notification notification = new Notification();
        notification.setRecipient(driver.getUser());
        notification.setEventCode(eventCode);
        notification.setContent(content);
        notification.setLink("/driver/my-route");
        notification.setSendStatus(NotificationSendStatus.SENT);
        notificationRepository.save(notification);
        return true;
    }

    private Route requireRoute(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến cần thao tác."));
    }
}
