package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByCode(String code);

    List<Route> findByDeliveryDate(LocalDate deliveryDate);

    // Chay lai phan tuyen thi xoa ket qua DRAFT truoc do cua cung ngay (BR03-09)
    List<Route> findByDeliveryDateAndStatus(LocalDate deliveryDate, RouteStatus status);

    // Tai xe chi thay tuyen cua chinh minh trong ngay hien tai (BR05-01).
    // Loc o tang du lieu chu khong chi an tren giao dien
    List<Route> findByDriverIdAndDeliveryDate(Long driverId, LocalDate deliveryDate);

    // Kiem tai xe da duoc gan tuyen nao trong ngay chua (CBR-06)
    boolean existsByDriverIdAndDeliveryDate(Long driverId, LocalDate deliveryDate);

    long countByDeliveryDate(LocalDate deliveryDate);
}
