package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Incident;
import com.minhphu.mproute.entity.Notification;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.IncidentSeverity;
import com.minhphu.mproute.entity.enums.IncidentStatus;
import com.minhphu.mproute.entity.enums.IncidentType;
import com.minhphu.mproute.entity.enums.NotificationSendStatus;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.StopStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.IncidentRepository;
import com.minhphu.mproute.repository.NotificationRepository;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.repository.RouteStopRepository;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tai xe bao su co tren duong (CN-06-01 -> CN-06-05, CN-06-13).
 *
 * HAI MUC DO DAN TOI HAI DUONG DI HOAN TOAN KHAC NHAU, va do la toan bo cai kho cua nhom nay:
 *
 *   CAN_CONTINUE    — xe van chay duoc, chi cham. He thong TU XU LY ngay: cong so phut cham
 *                     vao ETA cac diem con lai, bao khach gio moi, dong su co lai luon.
 *                     Dieu phoi vien khong phai lam gi (BR06-04).
 *
 *   CANNOT_CONTINUE — xe nam duong. He thong KHONG tu quyet: tuyen chuyen sang INCIDENT, xe
 *                     sang MAINTENANCE, va cho dieu phoi vien chon mot trong ba phuong an
 *                     (BR06-05, BR06-14). Chon phuong an la viec cua con nguoi vi no lien quan
 *                     toi dieu dong nguoi va xe that.
 *
 * Vi sao khong de he thong tu chon ca truong hop nang: ba phuong an co chi phi rat gan nhau ma
 * hau qua thi khac han — dieu them mot xe la them mot tai xe phai lam ngoai gio, con hoan sang
 * hom sau la mot cuoc goi xin loi khach. May khong biet hom nay ai ranh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DeliveryService deliveryService;
    private final StatusHistoryService statusHistoryService;
    private final CustomerNotificationService customerNotificationService;

    /** Ket qua mot lan bao su co, du de man hinh tai xe biet phai hien gi tiep theo. */
    public record ReportResult(Incident incident, int affectedStopCount, boolean needsDispatcher) {}

    /**
     * CN-06-02 — tai xe gui bieu mau bao su co.
     *
     * Di theo dung chieu cua BR05-01 nhu moi duong khac cua man hinh tai xe: khong nhan routeId
     * tu ben ngoai, ma tu tai khoan dang dang nhap tim ra tuyen hom nay.
     *
     * @param latitude, longitude toa do trinh duyet lay duoc, null neu tai xe tu choi quyen
     */
    @Transactional
    public ReportResult report(Long userId,
                               IncidentType incidentType,
                               IncidentSeverity severity,
                               String description,
                               Integer delayMinutes,
                               Double latitude,
                               Double longitude) {

        Route route = deliveryService.todayRouteOf(userId).orElseThrow(() ->
                new IllegalStateException("Hôm nay bạn chưa được giao tuyến nào."));

        /*
         * CN-06-01 — nut bao su co chi bam duoc khi tuyen DANG CHAY. Truoc do thi xe con o kho,
         * co gi tren duong dau ma su co; sau do thi tuyen da xong roi.
         */
        if (route.getStatus() != RouteStatus.IN_PROGRESS) {
            throw new IllegalStateException(String.format(
                    "Tuyến đang ở trạng thái \"%s\" nên không báo sự cố được.",
                    route.getStatus().getDisplayName()));
        }

        if (incidentType == null) {
            throw new IllegalArgumentException("Bắt buộc chọn loại sự cố.");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Bắt buộc chọn mức độ sự cố.");
        }
        // BR06-01 — chon "Khac" thi bat buoc mo ta, khong thi loai OTHER thanh thung rac
        if (incidentType == IncidentType.OTHER && (description == null || description.isBlank())) {
            throw new IllegalArgumentException("Chọn loại \"Khác\" thì bắt buộc mô tả cụ thể.");
        }
        // BR06-04 — muc nhe thi con so phut cham tre chinh la thu he thong dung de tinh lai ETA
        if (severity == IncidentSeverity.CAN_CONTINUE && (delayMinutes == null || delayMinutes <= 0)) {
            throw new IllegalArgumentException(
                    "Chọn \"Vẫn tiếp tục được\" thì bắt buộc nhập số phút chậm trễ.");
        }

        List<RouteStop> remainingStops = remainingStopsOf(route);

        Incident incident = new Incident();
        incident.setRoute(route);
        incident.setDriver(route.getDriver());
        incident.setIncidentType(incidentType);
        incident.setSeverity(severity);
        incident.setDescription(blankToNull(description));
        incident.setReportedAt(LocalDateTime.now());
        applyLocation(incident, route, latitude, longitude);

        if (severity == IncidentSeverity.CAN_CONTINUE) {
            incident.setDelayMinutes(delayMinutes);
            handleDelay(route, remainingStops, delayMinutes, incidentType);
            // Khong con gi cho dieu phoi vien quyet, dong lai luon
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setResolvedAt(LocalDateTime.now());
        } else {
            incident.setStatus(IncidentStatus.PENDING);
            handleBreakdown(route, userId);
        }

        Incident saved = incidentRepository.save(incident);
        notifyDispatchers(saved, route, remainingStops);

        log.info("Su co {} muc {} tren tuyen {}: {} diem con lai bi anh huong",
                incidentType, severity, route.getCode(), remainingStops.size());
        return new ReportResult(saved, remainingStops.size(),
                severity == IncidentSeverity.CANNOT_CONTINUE);
    }

    // ==================== CN-06-03 toa do ====================

    /**
     * BR06-02 — toa do noi xay ra su co.
     *
     * Trinh duyet tu choi quyen vi tri thi lay toa do cua DIEM GIAO GAN NHAT DA XONG va danh
     * dau estimated_location = true. Vi sao khong de trong: dieu phoi vien can biet xe dang o
     * DAU tren ban do de con dieu xe cuu ho toi — mot toa do gan dung van hon han khong co gi.
     * Co danh dau de tren man hinh xu ly ghi ro day la vi tri uoc luong, khong ai nham.
     */
    private void applyLocation(Incident incident, Route route, Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            incident.setLatitude(latitude);
            incident.setLongitude(longitude);
            incident.setEstimatedLocation(false);
            return;
        }

        RouteStop lastFinished = null;
        for (RouteStop stop : route.getStops()) {
            if (stop.getStatus() == StopStatus.DELIVERED
                    || stop.getStatus() == StopStatus.FAILED
                    || stop.getStatus() == StopStatus.POSTPONED) {
                lastFinished = stop;
            }
        }

        if (lastFinished != null) {
            incident.setLatitude(lastFinished.getOrder().getLatitude());
            incident.setLongitude(lastFinished.getOrder().getLongitude());
        } else {
            // Chua giao diem nao thi xe vua roi kho, lay luon toa do kho
            incident.setLatitude(route.getDepot().getLatitude());
            incident.setLongitude(route.getDepot().getLongitude());
        }
        incident.setEstimatedLocation(true);
    }

    // ==================== CN-06-04 muc nhe ====================

    /**
     * BR06-04 — cong so phut cham tre vao ETA cac diem CON LAI, roi bao khach gio moi.
     *
     * Chi dung toi cac diem chua giao. Diem da giao xong thi gio den cua no la chuyen da roi,
     * sua lai la lam sai lich su — va khach do cung khong quan tam nua.
     */
    private void handleDelay(Route route, List<RouteStop> remainingStops,
                             int delayMinutes, IncidentType incidentType) {

        for (RouteStop stop : remainingStops) {
            if (stop.getEstimatedArrival() != null) {
                stop.setEstimatedArrival(stop.getEstimatedArrival().plusMinutes(delayMinutes));
            }
        }
        routeStopRepository.saveAll(remainingStops);

        // Gui thu SAU khi da doi ETA, de thu mang dung con so moi (CN-06-12, NTF-07)
        customerNotificationService.notifyEtaChanged(remainingStops,
                incidentType.getDisplayName().toLowerCase(Locale.ROOT) + " trên đường");
    }

    // ==================== CN-06-13 xe hong ====================

    /**
     * BR06-14 — xe khong chay tiep duoc thi chuyen sang MAINTENANCE ngay.
     *
     * Doi den luc dieu phoi vien chon xong phuong an moi doi la muon: giua luc do co the co
     * nguoi chay phan tuyen cho ngay mai va he thong lai xep chinh chiec xe dang nam duong vao
     * ke hoach, vi man hinh phan tuyen chi loc theo AVAILABLE.
     *
     * Tuyen chuyen sang INCIDENT chu khong phai CANCELLED: cac diem da giao van giu nguyen ket
     * qua, va tuyen se quay lai IN_PROGRESS neu dieu phoi chon phuong an dieu xe cuu ho.
     */
    private void handleBreakdown(Route route, Long userId) {
        RouteStatus oldStatus = route.getStatus();
        route.setStatus(RouteStatus.INCIDENT);
        routeRepository.save(route);
        statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                oldStatus.name(), RouteStatus.INCIDENT.name(), userId,
                "Tai xe bao su co khong the tiep tuc");

        Vehicle vehicle = route.getVehicle();
        if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
            vehicle.setStatus(VehicleStatus.MAINTENANCE);
            vehicleRepository.save(vehicle);
        }
    }

    // ==================== CN-06-05 bao dieu phoi ====================

    /**
     * Thong bao khan cho dieu phoi vien, kem loai su co, vi tri va danh sach diem bi anh huong.
     *
     * Ghi ca ba thu vao mot dong noi dung chu khong bat dieu phoi vien mo ra xem: day la thong
     * bao KHAN, no phai doc duoc het ngay tren dong danh sach.
     */
    private void notifyDispatchers(Incident incident, Route route, List<RouteStop> remainingStops) {
        StringBuilder content = new StringBuilder();
        content.append(incident.getSeverity() == IncidentSeverity.CANNOT_CONTINUE ? "KHẨN: " : "");
        content.append("Tuyến ").append(route.getCode())
                .append(" báo sự cố ").append(incident.getIncidentType().getDisplayName().toLowerCase(Locale.ROOT))
                .append(" (").append(incident.getSeverity().getDisplayName().toLowerCase(Locale.ROOT)).append(")");

        if (incident.getDelayMinutes() != null) {
            content.append(", chậm ").append(incident.getDelayMinutes()).append(" phút");
        }
        content.append(". Vị trí ")
                .append(String.format(Locale.ROOT, "%.5f, %.5f",
                        incident.getLatitude(), incident.getLongitude()))
                .append(Boolean.TRUE.equals(incident.getEstimatedLocation()) ? " (ước lượng)" : "");

        content.append(". Còn ").append(remainingStops.size()).append(" điểm chưa giao");
        if (!remainingStops.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (RouteStop stop : remainingStops) {
                names.add(stop.getOrder().getCustomer().getName());
            }
            content.append(": ").append(String.join(", ", names));
        }
        content.append('.');

        for (User dispatcher : userRepository.findByRoleOrderByUsernameAsc(Role.DISPATCHER)) {
            Notification notification = new Notification();
            notification.setRecipient(dispatcher);
            notification.setEventCode("NTF-02");
            notification.setContent(content.toString());
            notification.setLink("/incidents/" + incident.getId());
            notification.setSendStatus(NotificationSendStatus.SENT);
            notificationRepository.save(notification);
        }
    }

    // ==================== Ham dung chung ====================

    /** Cac diem chua co ket qua cuoi cung — chinh la nhung diem mot su co lam anh huong toi. */
    private List<RouteStop> remainingStopsOf(Route route) {
        List<RouteStop> remaining = new ArrayList<>();
        for (RouteStop stop : route.getStops()) {
            if (stop.getStatus() == StopStatus.PENDING || stop.getStatus() == StopStatus.DELIVERING) {
                remaining.add(stop);
            }
        }
        return remaining;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
