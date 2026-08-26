package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.StatusHistory;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.RouteStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tra cuu don hang cho khach — trang cong khai /track (CN-07-01 -> CN-07-06).
 *
 * DAY LA LOP DUY NHAT CUA HE THONG TRA DU LIEU CHO NGUOI CHUA DANG NHAP, nen no duoc viet
 * theo mot nguyen tac khac han cac service con lai:
 *
 *     KHONG BAO GIO tra ve entity Order ra ngoai. Luon doi sang record TrackedOrder chi chua
 *     dung nhung truong duoc phep hien.
 *
 * Vi sao khong tra Order roi de file .html tu chon truong ma hien: lam vay thi ngay mai them
 * mot dong th:text la lo gia tri don hang, va khong ai phat hien ra. Cat o TANG DU LIEU thi
 * du lieu cam khong bao gio di toi duoc trinh duyet — co sua template cung khong lo duoc,
 * vi trong tay template khong con so tien nao ca (BR07-04).
 *
 * BON THU BI CAM theo BR07-04, doi chieu voi record ben duoi se thay khong co truong nao mang
 * chung ra: gia tri don hang, bien so xe, khach khac cung tuyen, cac diem giao khac.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    /** BR07-03 — tra bang so dien thoai chi duoc thay don trong 30 ngay gan nhat. */
    private static final int PHONE_LOOKUP_DAYS = 30;

    private final OrderRepository orderRepository;
    private final RouteStopRepository routeStopRepository;
    private final StatusHistoryService statusHistoryService;

    /** Mot buoc trong hanh trinh don hang (CN-07-04). */
    public record JourneyStep(LocalDateTime changedAt, String statusDisplay) {}

    /**
     * Don hang duoi goc nhin cua khach.
     *
     * CO Y KHONG CO: tong tien, so tien phai thu, bien so xe, ma tuyen, cac diem giao khac,
     * ten khach khac. Muon them truong nao vao day thi phai doi chieu lai BR07-04 truoc.
     *
     * driverName va driverPhone chi khac null khi don DANG GIAO (BR07-05) — luc khac de null
     * ngay tu tang service, khong phai de template tu quyet dinh an hay hien.
     */
    public record TrackedOrder(String code,
                               String customerName,
                               String deliveryAddress,
                               int boxCount,
                               LocalDate deliveryDate,
                               OrderStatus status,
                               LocalDateTime estimatedArrival,
                               LocalDateTime actualArrival,
                               String driverName,
                               String driverPhone,
                               List<JourneyStep> journey) {}

    /**
     * Tra cuu theo ma don hoac so dien thoai (CN-07-02, BR07-02).
     *
     * Phan biet hai kieu bang cach nhin co chu cai hay khong: ma don la "MP" + 10 chu so, con
     * so dien thoai thi toan so. Don gian va du dung, khong can bat nguoi dung chon kieu.
     *
     * Tra ve danh sach RONG khi khong tim thay — goi ben ngoai hien MSG-11 chung chung, khong
     * duoc noi ro la "khong co ma nay" hay "so nay khong co don" (BR07-08). Noi ro thi trang
     * nay thanh cong cu do xem so dien thoai nao co giao dich voi Minh Phu.
     */
    @Transactional(readOnly = true)
    public List<TrackedOrder> lookup(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return List.of();
        }
        String input = rawInput.trim();

        List<Order> found = input.matches("\\d+")
                ? orderRepository.findRecentByCustomerPhone(input, LocalDate.now().minusDays(PHONE_LOOKUP_DAYS))
                : orderRepository.findByCode(input.toUpperCase()).map(List::of).orElse(List.of());

        List<TrackedOrder> result = new ArrayList<>();
        for (Order order : found) {
            result.add(toTrackedOrder(order));
        }
        return result;
    }

    // ==================== Ham rieng ====================

    private TrackedOrder toTrackedOrder(Order order) {
        RouteStop stop = routeStopRepository.findByOrderId(order.getId()).orElse(null);

        /*
         * BR07-05 — ten va SDT tai xe CHI hien khi don dang tren duong giao.
         *
         * Cat ngay tai day chu khong de template tu an: don da giao xong hom qua ma van tra ve
         * SDT tai xe thi bat ky ai biet ma don cung goi thang cho anh ta duoc, va do la thong
         * tin ca nhan cua nhan vien chu khong phai cua don hang.
         */
        String driverName = null;
        String driverPhone = null;
        if (order.getStatus() == OrderStatus.DELIVERING && stop != null) {
            Route route = stop.getRoute();
            if (route != null && route.getDriver() != null) {
                driverName = route.getDriver().getFullName();
                driverPhone = route.getDriver().getPhone();
            }
        }

        int boxCount = order.getItems().stream().mapToInt(item -> item.getBoxCount()).sum();

        return new TrackedOrder(
                order.getCode(),
                order.getCustomer().getName(),
                order.getDeliveryAddress(),
                boxCount,
                order.getDeliveryDate(),
                order.getStatus(),
                (stop == null) ? null : stop.getEstimatedArrival(),
                (stop == null) ? null : stop.getActualArrival(),
                driverName,
                driverPhone,
                journeyOf(order));
    }

    /**
     * CN-07-04 — hanh trinh don hang theo thu tu thoi gian, lay tu status_history (BR07-06).
     *
     * CHI LAY THOI DIEM VA TEN TRANG THAI, khong lay cot note. Ghi chu la van ban noi bo, no
     * chua nhung thu nhu "Phan tuyen TT26082101" hay "Chay lai phan tuyen, huy ket qua nhap cu" —
     * ma tuyen va cach lam viec ben trong khong phai viec cua khach.
     */
    private List<JourneyStep> journeyOf(Order order) {
        List<JourneyStep> steps = new ArrayList<>();
        for (StatusHistory entry : statusHistoryService.historyOf(EntityType.ORDER, order.getId())) {
            steps.add(new JourneyStep(entry.getChangedAt(), displayNameOf(entry.getNewStatus())));
        }
        return steps;
    }

    /** Doi ma trang thai luu trong CSDL sang ten tieng Viet hien ra man hinh. */
    private String displayNameOf(String statusCode) {
        try {
            return OrderStatus.valueOf(statusCode).getDisplayName();
        } catch (IllegalArgumentException ex) {
            // Du lieu cu hoac enum da doi ten: hien nguyen ma con hon lam vo ca trang
            log.warn("Trang thai '{}' khong khop enum OrderStatus", statusCode);
            return statusCode;
        }
    }
}
