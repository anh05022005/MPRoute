package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Notification;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.OrderItem;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.FailureReason;
import com.minhphu.mproute.entity.enums.NotificationSendStatus;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.PaymentStatus;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.StopStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.DriverRepository;
import com.minhphu.mproute.repository.NotificationRepository;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.repository.RouteStopRepository;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Nghiep vu cua man hinh tai xe — SCR-12 (CN-05-01 -> CN-05-05).
 *
 * BR05-01 LA DIEU QUAN TRONG NHAT CUA LOP NAY: tai xe chi duoc thay tuyen CUA CHINH MINH,
 * trong NGAY HOM NAY. Phep loc do nam o day, tang service — khong phai o giao dien.
 * Man hinh tai xe khong co o nhap ma tuyen, nhung hoi dong se go thang dia chi
 * /driver/my-route/... voi id tuyen cua nguoi khac de thu, va luc do chi tang service moi
 * chan duoc (CBR-15).
 *
 * Cach chan: KHONG BAO GIO nhan routeId tu ben ngoai roi di tim tuyen do. Luon di theo chieu
 * nguoc lai — tu tai khoan dang dang nhap tim ra tai xe, tu tai xe tim ra tuyen cua ngay hom
 * nay. Nho vay khong co duong nao de tro toi tuyen cua nguoi khac, ke ca khi doan dung id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    /*
     * Tuyen o cac trang thai nay moi hien ra cho tai xe. Tuyen DRAFT co y bi loai:
     * ke hoach chua chot thi dieu phoi vien con chay lai thuat toan duoc, tai xe ma
     * nhin thay roi di giao truoc la giao theo mot ke hoach sap bi xoa.
     */
    private static final List<RouteStatus> VISIBLE_TO_DRIVER = List.of(
            RouteStatus.CONFIRMED, RouteStatus.IN_PROGRESS,
            RouteStatus.INCIDENT, RouteStatus.COMPLETED);

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final DriverRepository driverRepository;
    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final StatusHistoryService statusHistoryService;
    private final OrderService orderService;

    /** Ho so tai xe cua tai khoan dang dang nhap. Rong khi tai khoan chua duoc gan tai xe nao. */
    @Transactional(readOnly = true)
    public Optional<Driver> driverOf(Long userId) {
        return driverRepository.findByUserId(userId);
    }

    /**
     * CN-05-01 — tuyen hom nay cua tai xe dang dang nhap.
     *
     * Ngay lay tu dong ho cua UNG DUNG chu khong phai cua MySQL: may chu CSDL cua may nay chay
     * trong WSL nen dong ho la UTC, lech 7 tieng, dung CURDATE() thi tu 0h den 7h sang se ra
     * ngay hom truoc va tai xe mo dien thoai ra khong thay tuyen dau.
     */
    @Transactional(readOnly = true)
    public Optional<Route> todayRouteOf(Long userId) {
        return driverOf(userId).flatMap(driver -> routeOfDriverOn(driver.getId(), LocalDate.now()));
    }

    private Optional<Route> routeOfDriverOn(Long driverId, LocalDate date) {
        return routeRepository.findByDriverIdAndDeliveryDate(driverId, date).stream()
                .filter(route -> VISIBLE_TO_DRIVER.contains(route.getStatus()))
                .findFirst();
    }

    /**
     * CN-05-02 — tai xe xac nhan da nhan hang, tuyen chuyen sang IN_PROGRESS (BR05-03).
     *
     * Truoc khi bam nut nay thi khong cap nhat duoc diem giao nao. Cai chot do co ich that:
     * no phan biet "tuyen da len ke hoach" voi "xe da roi kho", va thoi diem departed_at chinh
     * la moc de doi chieu voi ETA khi tinh do chinh xac cua mo hinh o chuong 4.
     *
     * @param userId tai khoan dang dang nhap — tuyen phai thuoc ve chinh nguoi nay (BR05-01)
     */
    @Transactional
    public Route confirmPickup(Long userId) {
        Route route = todayRouteOf(userId).orElseThrow(() ->
                new IllegalStateException("Hôm nay bạn chưa được giao tuyến nào."));

        if (route.getStatus() == RouteStatus.IN_PROGRESS) {
            throw new IllegalStateException("Bạn đã xác nhận nhận hàng cho tuyến này rồi.");
        }
        if (route.getStatus() != RouteStatus.CONFIRMED) {
            throw new IllegalStateException(String.format(
                    "Tuyến đang ở trạng thái \"%s\" nên không xác nhận nhận hàng được.",
                    route.getStatus().getDisplayName()));
        }

        route.setStatus(RouteStatus.IN_PROGRESS);
        route.setDepartedAt(LocalDateTime.now());
        routeRepository.save(route);

        statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                RouteStatus.CONFIRMED.name(), RouteStatus.IN_PROGRESS.name(),
                userId, "Tai xe xac nhan nhan hang");

        log.info("Tuyen {} chuyen sang IN_PROGRESS luc {}", route.getCode(), route.getDepartedAt());
        return route;
    }

    /**
     * CN-05-03, BR05-04, BR05-05 — diem tai xe phai xu ly NGAY BAY GIO.
     *
     * Dung diem CHUA KET THUC dau tien theo thu tu sequence. Chi co dung mot diem duoc lam noi
     * bat tren man hinh, va tai xe khong duoc nhay coc sang diem sau — quy tac do khien man hinh
     * chi con mot viec de lam tai mot thoi diem, dung voi hoan canh dang ngoi tren xe.
     *
     * Tra ve rong khi moi diem da xong, hoac khi tuyen chua duoc xac nhan nhan hang (BR05-03).
     */
    public Optional<RouteStop> currentStopOf(Route route) {
        if (route == null || route.getStatus() != RouteStatus.IN_PROGRESS) {
            return Optional.empty();
        }
        return route.getStops().stream()
                .filter(stop -> stop.getStatus() == StopStatus.PENDING
                        || stop.getStatus() == StopStatus.DELIVERING)
                .findFirst();
    }

    /** So diem da co ket qua cuoi cung, de hien thanh tien do "1 / 3 diem". */
    public long finishedStopCount(Route route) {
        if (route == null) {
            return 0;
        }
        return route.getStops().stream()
                .filter(stop -> stop.getStatus() != StopStatus.PENDING
                        && stop.getStatus() != StopStatus.DELIVERING)
                .count();
    }

    // ==================== CN-05-06 -> CN-05-10 cap nhat ket qua giao ====================

    /**
     * CN-05-06, CN-05-07 — tai xe bam "Da giao".
     *
     * SO TIEN THU LA PHAN DE SAI NHAT o day, va sai thi sai vao tien mat cua doanh nghiep:
     *   - don co amount_to_collect > 0 thi BAT BUOC nhap so thuc thu (BR05-11)
     *   - thu khac so phai thu thi BAT BUOC ghi ly do va bao ngay cho dieu phoi (BR05-12)
     * He thong chi GHI NHAN chu khong quan ly dong tien, nhung ghi nhan sai thi cuoi ngay
     * doi soat khong ra.
     *
     * @param collectedAmount so tien tai xe thuc thu, null khi don khong phai thu gi
     * @param latitude, longitude toa do luc bam nut, null neu trinh duyet khong cho (BR05-08)
     */
    @Transactional
    public RouteStop markDelivered(Long userId, Long stopId,
                                   BigDecimal collectedAmount, String collectionNote,
                                   Double latitude, Double longitude) {

        RouteStop stop = requireCurrentStop(userId, stopId);
        Order order = stop.getOrder();
        BigDecimal amountToCollect = order.getAmountToCollect();

        if (amountToCollect.signum() > 0) {
            // BR05-11
            if (collectedAmount == null) {
                throw new IllegalArgumentException(
                        "Đơn này phải thu tiền nên bắt buộc nhập số tiền thực thu.");
            }
            if (collectedAmount.signum() < 0) {
                throw new IllegalArgumentException("Số tiền thực thu không được âm.");
            }

            // BR05-12 — thu khac so phai thu thi phai giai trinh, khong duoc de troi
            if (collectedAmount.compareTo(amountToCollect) != 0) {
                if (collectionNote == null || collectionNote.isBlank()) {
                    throw new IllegalArgumentException(
                            "Số tiền thu khác số phải thu nên bắt buộc ghi lý do.");
                }
                notifyDispatchers("NTF-09", String.format(
                        "Đơn %s (%s) thu %s trong khi phải thu %s. Lý do: %s",
                        order.getCode(), order.getCustomer().getName(),
                        formatMoney(collectedAmount), formatMoney(amountToCollect),
                        collectionNote.trim()));
            }

            stop.setCollectedAmount(collectedAmount);
            stop.setCollectionNote(blankToNull(collectionNote));
        }

        stop.setStatus(StopStatus.DELIVERED);
        recordArrival(stop, latitude, longitude);

        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(paymentStatusAfterDelivery(order));
        orderRepository.save(order);
        statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                OrderStatus.ROUTED.name(), OrderStatus.DELIVERED.name(), userId, "Tai xe giao thanh cong");

        finishRouteIfDone(stop.getRoute(), userId);
        return stop;
    }

    /**
     * Trang thai thanh toan sau khi giao thanh cong.
     *
     * CREDIT thanh DEBT chu khong phai PAID (BR05-14): hang da giao nhung tien thi ghi so,
     * de nguyen UNPAID thi khong phan biet duoc voi don chua giao, ma dat PAID thi bao cao
     * cong no bang 0 trong khi thuc te khach dang no.
     */
    private PaymentStatus paymentStatusAfterDelivery(Order order) {
        return switch (order.getPaymentMethod()) {
            case CREDIT -> PaymentStatus.DEBT;
            case CASH, PREPAID -> PaymentStatus.PAID;
        };
    }

    /**
     * CN-05-08 — giao that bai (BR05-06).
     *
     * Bat buoc chon ly do tu danh sach chu khong cho go tu do: bao cao RPT-02 gom nhom theo
     * ly do, ma go tu do thi khong gom duoc. Rieng OTHER thi bat nhap mo ta, neu khong chon
     * OTHER thanh cai thung rac ma khong ai biet ben trong la gi.
     */
    @Transactional
    public RouteStop markFailed(Long userId, Long stopId, FailureReason reason, String note,
                                Double latitude, Double longitude) {

        RouteStop stop = requireCurrentStop(userId, stopId);

        if (reason == null) {
            throw new IllegalArgumentException("Bắt buộc chọn lý do giao thất bại.");
        }
        if (reason == FailureReason.OTHER && (note == null || note.isBlank())) {
            throw new IllegalArgumentException("Chọn lý do \"Khác\" thì bắt buộc mô tả cụ thể.");
        }

        Order order = stop.getOrder();
        stop.setStatus(StopStatus.FAILED);
        stop.setFailureReason(reason);
        stop.setFailureNote(blankToNull(note));
        recordArrival(stop, latitude, longitude);

        // BR05-13: don that bai thi tien van chua thu, giu nguyen UNPAID
        order.setStatus(OrderStatus.FAILED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        orderRepository.save(order);
        statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                OrderStatus.ROUTED.name(), OrderStatus.FAILED.name(), userId,
                "Giao that bai: " + reason.getDisplayName());

        // NTF-03 — dieu phoi phai biet ngay de con goi lai cho khach trong ngay
        notifyDispatchers("NTF-03", String.format("Đơn %s (%s) giao thất bại: %s.%s",
                order.getCode(), order.getCustomer().getName(), reason.getDisplayName(),
                (note == null || note.isBlank()) ? "" : " " + note.trim()));

        finishRouteIfDone(stop.getRoute(), userId);
        return stop;
    }

    /**
     * CN-05-09 — hoan sang ngay khac (BR05-07).
     *
     * Don cu chuyen POSTPONED va TAO MOT DON MOI o ngay giao moi chu khong doi ngay cua don
     * cu. Ly do: doi ngay thi mat dau vet lan giao hut — bao cao RPT-02 khong con thay lan
     * hoan nao, va diem dung cua tuyen hom nay tro toi mot don co ngay giao khac han.
     * Don moi vao thang APPROVED vi don goc da duoc duyet roi, bat duyet lai la thua.
     */
    @Transactional
    public RouteStop postpone(Long userId, Long stopId, LocalDate newDeliveryDate, String note,
                              Double latitude, Double longitude) {

        RouteStop stop = requireCurrentStop(userId, stopId);

        if (newDeliveryDate == null) {
            throw new IllegalArgumentException("Bắt buộc chọn ngày giao mới.");
        }
        if (newDeliveryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày giao mới không được là ngày trong quá khứ.");
        }

        Order order = stop.getOrder();
        stop.setStatus(StopStatus.POSTPONED);
        stop.setFailureNote(blankToNull(note));
        recordArrival(stop, latitude, longitude);

        // BR05-13: hoan thi tien cung chua thu
        order.setStatus(OrderStatus.POSTPONED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        orderRepository.save(order);
        statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                OrderStatus.ROUTED.name(), OrderStatus.POSTPONED.name(), userId,
                "Hoan sang ngay " + newDeliveryDate);

        Order reissued = reissueForNewDate(order, newDeliveryDate, userId);

        notifyDispatchers("NTF-06", String.format(
                "Đơn %s (%s) được hoãn sang ngày %s, đã tạo lại thành đơn %s.",
                order.getCode(), order.getCustomer().getName(),
                newDeliveryDate.format(DAY_MONTH_YEAR), reissued.getCode()));

        finishRouteIfDone(stop.getRoute(), userId);
        return stop;
    }

    /** Tao lai don o ngay giao moi, chep nguyen chi tiet hang cua don cu. */
    private Order reissueForNewDate(Order source, LocalDate newDeliveryDate, Long userId) {
        Order copy = new Order();
        copy.setCode(orderService.generateOrderCode(newDeliveryDate));
        copy.setCustomer(source.getCustomer());
        copy.setDeliveryDate(newDeliveryDate);
        copy.setDeliveryAddress(source.getDeliveryAddress());
        copy.setLatitude(source.getLatitude());
        copy.setLongitude(source.getLongitude());
        copy.setPaymentMethod(source.getPaymentMethod());
        copy.setPaymentStatus(PaymentStatus.UNPAID);
        copy.setStatus(OrderStatus.APPROVED);
        copy.setNote(source.getNote());
        copy.setTotalWeight(source.getTotalWeight());
        copy.setTotalVolume(source.getTotalVolume());
        copy.setTotalAmount(source.getTotalAmount());
        copy.setAmountToCollect(source.getAmountToCollect());

        for (OrderItem line : source.getItems()) {
            OrderItem lineCopy = new OrderItem();
            lineCopy.setProduct(line.getProduct());
            lineCopy.setBoxCount(line.getBoxCount());
            lineCopy.setLineWeight(line.getLineWeight());
            lineCopy.setLineVolume(line.getLineVolume());
            lineCopy.setUnitPrice(line.getUnitPrice());
            lineCopy.setLineTotal(line.getLineTotal());
            copy.addItem(lineCopy);
        }

        Order saved = orderRepository.save(copy);
        statusHistoryService.recordChange(EntityType.ORDER, saved.getId(),
                null, OrderStatus.APPROVED.name(), userId,
                "Tao lai tu don hoan " + source.getCode());
        return saved;
    }

    /**
     * CN-05-10, BR05-09 — diem cuoi cung co ket qua thi tuyen tu hoan thanh.
     *
     * Tra xe ve AVAILABLE ngay tai day. Quen buoc nay thi chiec xe do bi khoa vinh vien:
     * khong con tuyen nao dang giu no, ma man hinh phan tuyen chi liet ke xe AVAILABLE nen
     * hom sau khong ai dua no vao ke hoach duoc nua.
     */
    private void finishRouteIfDone(Route route, Long userId) {
        boolean stillWorking = route.getStops().stream()
                .anyMatch(stop -> stop.getStatus() == StopStatus.PENDING
                        || stop.getStatus() == StopStatus.DELIVERING);
        if (stillWorking) {
            return;
        }

        route.setStatus(RouteStatus.COMPLETED);
        route.setCompletedAt(LocalDateTime.now());
        routeRepository.save(route);
        statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                RouteStatus.IN_PROGRESS.name(), RouteStatus.COMPLETED.name(), userId,
                "Diem cuoi da co ket qua");

        Vehicle vehicle = route.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.IN_USE) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        log.info("Tuyen {} hoan thanh luc {}, xe {} ve san sang",
                route.getCode(), route.getCompletedAt(), vehicle.getLicensePlate());
    }

    /** Tong ket mot tuyen da chay xong, hien ra man hinh cuoi ngay (CN-05-10). */
    public record RouteSummary(long deliveredCount,
                               long failedCount,
                               long postponedCount,
                               BigDecimal collectedAmount) {}

    /**
     * Con so cuoi ngay cua tai xe.
     *
     * Tong tien thu cong tu tung diem dung chu khong cong tu don hang: so tren diem dung la
     * so tai xe THUC SU thu duoc, con so tren don la so PHAI thu. Hai so nay lech nhau chinh
     * la truong hop BR05-12, va cuoi ngay doi soat quy thi phai lay so thuc thu.
     */
    public RouteSummary summaryOf(Route route) {
        if (route == null) {
            return new RouteSummary(0, 0, 0, BigDecimal.ZERO);
        }

        BigDecimal collected = BigDecimal.ZERO;
        for (RouteStop stop : route.getStops()) {
            if (stop.getStatus() == StopStatus.DELIVERED && stop.getCollectedAmount() != null) {
                collected = collected.add(stop.getCollectedAmount());
            }
        }

        return new RouteSummary(
                countByStatus(route, StopStatus.DELIVERED),
                countByStatus(route, StopStatus.FAILED),
                countByStatus(route, StopStatus.POSTPONED),
                collected);
    }

    private long countByStatus(Route route, StopStatus status) {
        return route.getStops().stream().filter(stop -> stop.getStatus() == status).count();
    }

    // ==================== Ham rieng ====================

    /**
     * Lay diem dung ma tai xe DUOC PHEP cap nhat ngay bay gio.
     *
     * Gom ba phep kiem trong mot cho, va ca ba deu bat buoc phai o tang service:
     *   1. tuyen phai thuoc ve chinh tai xe dang dang nhap (BR05-01)
     *   2. tuyen phai dang IN_PROGRESS, tuc da bam nhan hang (BR05-03)
     *   3. diem gui len phai DUNG la diem dang toi luot — khong nhay coc (BR05-05)
     *
     * Phep kiem thu ba la thu de bi bo qua nhat vi giao dien chi hien nut o dung mot diem.
     * Nhung gui thang stopId cua diem thu ba len thi van phai bi tu choi: khong co no thi
     * tai xe bam nham mot cai la thu tu giao loan, ma tuyen lai duoc toi uu theo dung thu tu do.
     */
    private RouteStop requireCurrentStop(Long userId, Long stopId) {
        Route route = todayRouteOf(userId).orElseThrow(() ->
                new IllegalStateException("Hôm nay bạn chưa được giao tuyến nào."));

        if (route.getStatus() != RouteStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Bạn phải bấm \"Xác nhận đã nhận hàng\" trước khi cập nhật điểm giao.");
        }

        RouteStop current = currentStopOf(route).orElseThrow(() ->
                new IllegalStateException("Tuyến này đã cập nhật xong mọi điểm giao."));

        if (!current.getId().equals(stopId)) {
            throw new IllegalStateException(String.format(
                    "Phải cập nhật theo đúng thứ tự. Điểm đang tới lượt là điểm %d — %s.",
                    current.getSequence(), current.getOrder().getCustomer().getName()));
        }
        return current;
    }

    /**
     * Ghi thoi diem va toa do luc tai xe bam nut (BR05-08).
     *
     * Toa do co the null va do la truong hop BINH THUONG: trinh duyet co the tu choi quyen
     * vi tri, hoac dien thoai dang trong ham. Khong duoc vi thieu toa do ma chan viec cap
     * nhat — tai xe van dang dung truoc cua hang va van phai bao ket qua ve.
     */
    private void recordArrival(RouteStop stop, Double latitude, Double longitude) {
        stop.setActualArrival(LocalDateTime.now());
        stop.setUpdatedLatitude(latitude);
        stop.setUpdatedLongitude(longitude);
        routeStopRepository.save(stop);
    }

    /** Gui thong bao trong he thong cho moi dieu phoi vien. */
    private void notifyDispatchers(String eventCode, String content) {
        for (User dispatcher : userRepository.findByRoleOrderByUsernameAsc(Role.DISPATCHER)) {
            Notification notification = new Notification();
            notification.setRecipient(dispatcher);
            notification.setEventCode(eventCode);
            notification.setContent(content);
            notification.setLink("/tracking");
            notification.setSendStatus(NotificationSendStatus.SENT);
            notificationRepository.save(notification);
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /**
     * Tien theo quy uoc hien thi cua do an: dau cham ngan hang nghin, hau to "d" (docs/05).
     *
     * Dung DecimalFormatSymbols dat tay chu khong dua vao locale cua may: may cai dat tieng Anh
     * se ra "5,000,000d", may cai tieng Viet ra "5.000.000d" — cung mot dong code ma hai may cho
     * hai ket qua khac nhau la loai loi chi lo ra khi doi may, dung luc sap bao ve.
     */
    private static String formatMoney(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + "đ";
    }

    /**
     * Quang duong con lai cua tuyen, met — cong cac chang cua nhung diem chua giao xong,
     * VA chang cuoi tu diem cuoi ve kho (CBR-01).
     *
     * Chang ve kho phai cong vao: tai xe con phai chay ve kho tra tien va do xe, do la quang
     * duong that ma anh ta con phai di. Lay tong tuyen tru di cac chang da di xong thi ra dung
     * so nay ma khong phai tra lai ma tran khoang cach.
     */
    public double remainingMeters(Route route) {
        if (route == null) {
            return 0;
        }

        double finishedLegs = route.getStops().stream()
                .filter(stop -> stop.getStatus() != StopStatus.PENDING
                        && stop.getStatus() != StopStatus.DELIVERING)
                .mapToDouble(RouteStop::getLegDistance)
                .sum();

        return Math.max(route.getTotalDistance() - finishedLegs, 0);
    }
}
