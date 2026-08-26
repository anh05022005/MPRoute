package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Notification;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.Vehicle;
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
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;
    private final StatusHistoryService statusHistoryService;

    // ==================== Doc ====================

    /**
     * Tai xe con chon duoc cho mot tuyen trong ngay (BR04-07, CBR-06).
     *
     * Loai san nhung nguoi da co tuyen khac cung ngay ra khoi o chon, chu khong de nguoi dung
     * chon xong roi moi bao loi. Tai xe cua CHINH tuyen dang xet thi giu lai, neu khong thi mo
     * o chon ra la thay trong, tuong nhu chua gan ai.
     */
    @Transactional(readOnly = true)
    public List<Driver> selectableDriversFor(LocalDate deliveryDate, Route route) {
        Set<Long> takenDriverIds = new HashSet<>();
        for (Route other : routeRepository.findByDeliveryDate(deliveryDate)) {
            if (other.getStatus() == RouteStatus.CANCELLED || other.getDriver() == null) {
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
         * CBR-06 — mot tai xe toi da mot tuyen mot ngay. Kiem lai o day chu khong tin vao o
         * chon: gui thang driverId cua nguoi da co tuyen len thi van phai bi tu choi.
         */
        for (Route other : routeRepository.findByDeliveryDate(route.getDeliveryDate())) {
            if (other.getId().equals(route.getId()) || other.getStatus() == RouteStatus.CANCELLED) {
                continue;
            }
            if (other.getDriver() != null && other.getDriver().getId().equals(driverId)) {
                throw new IllegalStateException(String.format(
                        "Tài xế %s đã được giao tuyến %s trong ngày này rồi.",
                        driver.getFullName(), other.getCode()));
            }
        }

        route.setDriver(driver);
        log.info("Tuyen {} giao cho tai xe {} ({})", route.getCode(), driver.getFullName(), driverId);
        return routeRepository.save(route);
    }

    // ==================== CN-04-07 chot ke hoach ====================

    /** So tuyen da chot va so thong bao da gui, de bao lai cho dieu phoi vien. */
    public record ConfirmResult(int routeCount, int notifiedDriverCount) {}

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

        List<Order> stillUnassigned =
                orderRepository.findByDeliveryDateAndStatus(deliveryDate, OrderStatus.APPROVED);
        if (!stillUnassigned.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Còn %d đơn chưa xếp được vào tuyến nào. Chọn thêm xe và chạy lại trước khi chốt.",
                    stillUnassigned.size()));
        }

        int notified = 0;
        for (Route route : draftRoutes) {
            route.setStatus(RouteStatus.CONFIRMED);
            routeRepository.save(route);
            statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                    RouteStatus.DRAFT.name(), RouteStatus.CONFIRMED.name(),
                    currentUserId, "Chot ke hoach giao ngay " + deliveryDate);

            /*
             * Xe bi khoa NGAY LUC CHOT chu khong doi tai xe nhan hang (docs/01 muc trang thai xe):
             * ke hoach da chot thi chiec xe do coi nhu da co viec, khong duoc dua vao lan phan
             * tuyen nao khac nua. Tuyen thi van la CONFIRMED cho toi khi tai xe bam nhan hang.
             */
            Vehicle vehicle = route.getVehicle();
            if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
                vehicle.setStatus(VehicleStatus.IN_USE);
                vehicleRepository.save(vehicle);
            }

            if (notifyDriver(route, "NTF-01", String.format(
                    "Tuyến %s ngày %s đã được chốt. Bạn có %d điểm giao.",
                    route.getCode(), deliveryDate.format(DAY_MONTH_YEAR), route.getStops().size()))) {
                notified++;
            }
        }

        log.info("Chot ke hoach ngay {}: {} tuyen sang CONFIRMED, {} tai xe nhan thong bao",
                deliveryDate, draftRoutes.size(), notified);
        return new ConfirmResult(draftRoutes.size(), notified);
    }

    // ==================== CN-04-09 huy tuyen ====================

    /**
     * Huy mot tuyen (CN-04-09, BR04-09, BR04-10).
     *
     * Tra don ve APPROVED va xe ve AVAILABLE — dung thu tu nguoc lai voi luc chot. Khong tra don
     * ve thi cac don do treo o ROUTED ma khong tuyen nao chua, khong bao gio duoc phan tuyen lai.
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

        // Xe chi ve AVAILABLE khi chinh tuyen nay dang giu no. Tuyen nhap thi xe von van ranh
        Vehicle vehicle = route.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.IN_USE) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        route.setStatus(RouteStatus.CANCELLED);
        routeRepository.save(route);
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
