package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.enums.StopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByRouteIdOrderBySequenceAsc(Long routeId);

    // Trang tra cuu cong khai tim diem dung theo don de lay ETA moi nhat (BR07-07)
    Optional<RouteStop> findByOrderId(Long orderId);

    // Xu ly su co: lay cac diem con lai chua giao de sinh phuong an
    List<RouteStop> findByRouteIdAndStatusOrderBySequenceAsc(Long routeId, StopStatus status);
}
