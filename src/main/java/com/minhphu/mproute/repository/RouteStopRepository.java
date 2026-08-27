package com.minhphu.mproute.repository;

import com.minhphu.mproute.dto.FailureReasonCount;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.enums.StopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByRouteIdOrderBySequenceAsc(Long routeId);

    // Trang tra cuu cong khai tim diem dung theo don de lay ETA moi nhat (BR07-07)
    Optional<RouteStop> findByOrderId(Long orderId);

    // Xu ly su co: lay cac diem con lai chua giao de sinh phuong an
    List<RouteStop> findByRouteIdAndStatusOrderBySequenceAsc(Long routeId, StopStatus status);

    /**
     * So lan giao that bai theo tung ly do — phan tich cua bao cao RPT-02 (CN-08-02).
     *
     * BO TUYEN DA HUY (BR08-04): tuyen huy thi khong ai di giao, cac diem cua no khong phai
     * lan giao that bai.
     *
     * Dem tren ROUTE_STOPS chu khong tren ORDERS: ly do that bai thuoc ve LAN GIAO. Mot don
     * hoan roi giao lai va lai hong la hai lan that bai voi hai ly do, ma cot trang thai cua
     * don chi giu duoc cai cuoi cung.
     */
    @Query("""
            SELECT new com.minhphu.mproute.dto.FailureReasonCount(s.failureReason, COUNT(s))
            FROM RouteStop s
            JOIN s.route r
            WHERE r.deliveryDate BETWEEN :fromDate AND :toDate
              AND s.status = com.minhphu.mproute.entity.enums.StopStatus.FAILED
              AND s.failureReason IS NOT NULL
              AND r.status <> com.minhphu.mproute.entity.enums.RouteStatus.CANCELLED
            GROUP BY s.failureReason
            ORDER BY COUNT(s) DESC
            """)
    List<FailureReasonCount> countFailuresByReason(@Param("fromDate") LocalDate fromDate,
                                                   @Param("toDate") LocalDate toDate);
}
