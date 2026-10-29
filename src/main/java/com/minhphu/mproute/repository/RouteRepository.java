package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByCode(String code);

    List<Route> findByDeliveryDate(LocalDate deliveryDate);

    // Chay lai phan tuyen thi xoa ket qua DRAFT truoc do cua cung ngay (BR03-09)
    List<Route> findByDeliveryDateAndStatus(LocalDate deliveryDate, RouteStatus status);

    /**
     * Cac tuyen kem san diem dung, don, khach, xe va tai xe — dung cho bang ket qua phan tuyen
     * (CN-03-07) va cho khoi gan tai xe (CN-04-06).
     *
     * JOIN FETCH vi bang ket qua hien ten khach o TUNG diem dung: khong nap san thi moi diem
     * dung sinh them mot cau SELECT rieng (van de N+1), ba tuyen 30 diem la 30 cau.
     *
     * Nhan mot DANH SACH trang thai chu khong phai mot trang thai: man hinh phai hien ca tuyen
     * nhap lan tuyen da chot, neu khong thi bam Chot ke hoach xong la ca bang bien mat.
     *
     * Chi mot quan he tap hop duoc FETCH (r.stops). Them mot cai nua, vi du o.items, la
     * Hibernate nem MultipleBagFetchException ngay luc khoi dong.
     */
    @Query("""
            SELECT r FROM Route r
            JOIN FETCH r.vehicle
            LEFT JOIN FETCH r.driver
            LEFT JOIN FETCH r.stops s
            LEFT JOIN FETCH s.order o
            LEFT JOIN FETCH o.customer
            WHERE r.deliveryDate = :deliveryDate AND r.status IN :statuses
            """)
    List<Route> findWithStopsByDeliveryDateAndStatusIn(@Param("deliveryDate") LocalDate deliveryDate,
                                                       @Param("statuses") Collection<RouteStatus> statuses);

    // Tai xe chi thay tuyen cua chinh minh trong ngay hien tai (BR05-01).
    // Loc o tang du lieu chu khong chi an tren giao dien
    List<Route> findByDriverIdAndDeliveryDate(Long driverId, LocalDate deliveryDate);

    // Kiem tai xe da duoc gan tuyen nao trong ngay chua (CBR-06)
    boolean existsByDriverIdAndDeliveryDate(Long driverId, LocalDate deliveryDate);

    // Xe nao da co viec trong ngay (CBR-05). Tra ve id de loc ca doi xe bang MOT cau truy van
    @Query("""
            SELECT DISTINCT r.vehicle.id FROM Route r
            WHERE r.deliveryDate = :deliveryDate AND r.status IN :statuses
            """)
    List<Long> findVehicleIdsByDeliveryDateAndStatusIn(@Param("deliveryDate") LocalDate deliveryDate,
                                                       @Param("statuses") Collection<RouteStatus> statuses);

    boolean existsByVehicleIdAndDeliveryDateAndStatusIn(Long vehicleId, LocalDate deliveryDate,
                                                        Collection<RouteStatus> statuses);

    // Xe con dang ngoai duong voi mot tuyen KHAC khong — truoc khi tra xe ve San sang
    boolean existsByVehicleIdAndStatusInAndIdNot(Long vehicleId, Collection<RouteStatus> statuses,
                                                 Long routeId);

    /**
     * Lich su cac tuyen tai xe da chay, moi nhat truoc (CN-05-11).
     *
     * Loc theo driver.id ngay trong cau truy van chu khong lay het roi loc trong Java:
     * tai xe chi duoc thay tuyen cua chinh minh, va cho chan chac nhat la o tang du lieu
     * (BR05-01).
     *
     * Bo tuyen da huy: tuyen do khong duoc chay ngay nao ca, va sau khi huy thi cac diem
     * dung cung da bi xoa nen hien ra chi la mot dong 0/0 vo nghia.
     *
     * JOIN FETCH stops vi moi dong deu can dem so diem giao thanh cong — khong nap san thi
     * moi tuyen sinh them mot cau SELECT rieng (van de N+1).
     */
    @Query("""
            SELECT r FROM Route r
            JOIN FETCH r.vehicle
            LEFT JOIN FETCH r.stops
            WHERE r.driver.id = :driverId
              AND r.deliveryDate < :beforeDate
              AND r.status <> com.minhphu.mproute.entity.enums.RouteStatus.CANCELLED
            ORDER BY r.deliveryDate DESC
            """)
    List<Route> findHistoryOfDriver(@Param("driverId") Long driverId,
                                    @Param("beforeDate") LocalDate beforeDate);

    long countByDeliveryDate(LocalDate deliveryDate);

    /**
     * Cac ngay trong khoang van con it nhat MOT tuyen chua bi huy — dung cho bao cao
     * RPT-03 de bo di nhung ngay ma moi tuyen deu da huy (BR08-04, CN-08-04).
     *
     * Tra ve DANH SACH NGAY chu khong tra ve tuyen: bao cao chi can biet ngay do co
     * chuyen hang nao thuc su chay khong. Gom nhom ngay trong SQL de ca khoang 365
     * ngay van chi ton mot cau truy van, thay vi hoi lai CSDL cho tung ngay.
     */
    @Query("""
            SELECT DISTINCT r.deliveryDate FROM Route r
            WHERE r.deliveryDate BETWEEN :fromDate AND :toDate
              AND r.status <> com.minhphu.mproute.entity.enums.RouteStatus.CANCELLED
            """)
    List<LocalDate> findDatesWithLiveRoutes(@Param("fromDate") LocalDate fromDate,
                                            @Param("toDate") LocalDate toDate);
}
