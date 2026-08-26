package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.StopStatus;
import com.minhphu.mproute.repository.DriverRepository;
import com.minhphu.mproute.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final StatusHistoryService statusHistoryService;

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
