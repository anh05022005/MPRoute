package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByCode(String code);

    List<Route> findByDeliveryDate(LocalDate deliveryDate);

    // Chay lai phan tuyen thi xoa ket qua DRAFT truoc do cua cung ngay (BR03-09)
    List<Route> findByDeliveryDateAndStatus(LocalDate deliveryDate, RouteStatus status);

    /**
     * Cac tuyen kem san diem dung, don va khach — dung cho bang ket qua phan tuyen (CN-03-07).
     *
     * JOIN FETCH vi bang ket qua hien ten khach o TUNG diem dung: khong nap san thi moi
     * diem dung sinh them mot cau SELECT rieng (van de N+1), ba tuyen 30 diem la 30 cau.
     */
    @Query("""
            SELECT r FROM Route r
            JOIN FETCH r.vehicle
            LEFT JOIN FETCH r.stops s
            LEFT JOIN FETCH s.order o
            LEFT JOIN FETCH o.customer
            WHERE r.deliveryDate = :deliveryDate AND r.status = :status
            """)
    List<Route> findWithStopsByDeliveryDateAndStatus(@Param("deliveryDate") LocalDate deliveryDate,
                                                     @Param("status") RouteStatus status);

    // Tai xe chi thay tuyen cua chinh minh trong ngay hien tai (BR05-01).
    // Loc o tang du lieu chu khong chi an tren giao dien
    List<Route> findByDriverIdAndDeliveryDate(Long driverId, LocalDate deliveryDate);

    // Kiem tai xe da duoc gan tuyen nao trong ngay chua (CBR-06)
    boolean existsByDriverIdAndDeliveryDate(Long driverId, LocalDate deliveryDate);

    long countByDeliveryDate(LocalDate deliveryDate);
}
