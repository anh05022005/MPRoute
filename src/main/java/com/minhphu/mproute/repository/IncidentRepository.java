package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Incident;
import com.minhphu.mproute.entity.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByRouteId(Long routeId);

    // Man hinh dieu phoi: cac su co dang cho chon phuong an
    List<Incident> findByStatusOrderByReportedAtDesc(IncidentStatus status);

    List<Incident> findByReportedAtBetween(LocalDateTime fromTime, LocalDateTime toTime);
}
