package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.IncidentSeverity;
import com.minhphu.mproute.entity.enums.IncidentStatus;
import com.minhphu.mproute.entity.enums.IncidentType;
import com.minhphu.mproute.entity.enums.SolutionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Su co tren duong do tai xe bao.
 *
 * CAN_CONTINUE: chi cong delayMinutes vao ETA cac diem con lai (BR06-04).
 * CANNOT_CONTINUE: sinh 3 nhom phuong an xep theo km phat sinh tang dan (BR06-05),
 * va tu chuyen xe sang MAINTENANCE (BR06-14).
 */
@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "incident_type", nullable = false, length = 30)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "severity", nullable = false, length = 30)
    private IncidentSeverity severity;

    @Column(name = "description", length = 500)
    private String description;

    // Chi dung khi severity = CAN_CONTINUE
    @Column(name = "delay_minutes")
    private Integer delayMinutes;

    @Column(name = "latitude", columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(name = "longitude", columnDefinition = "DECIMAL(10,7)")
    private Double longitude;

    // TRUE khi khong lay duoc GPS, phai dung toa do diem giao gan nhat da xong (BR06-02)
    @Column(name = "estimated_location", nullable = false)
    private Boolean estimatedLocation = false;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "applied_solution", length = 30)
    private SolutionType appliedSolution;

    @Column(name = "solution_note", length = 500)
    private String solutionNote;

    // Km phat sinh cua phuong an da chon, luu bang MET cho thong nhat
    @Column(name = "extra_distance")
    private Double extraDistance;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.PENDING;
}
