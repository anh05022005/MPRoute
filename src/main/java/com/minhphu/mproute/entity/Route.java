package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.entity.enums.RouteStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mot tuyen giao hang cua mot xe trong mot ngay.
 *
 * Bang dat ten "routes" so nhieu vi ROUTE la tu khoa SQL.
 *
 * totalDistance luu bang MET va DA GOM CHANG CUOI VE DEPOT (CBR-01).
 * Day la loi kinh dien cua do an VRP: quen chang cuoi thi moi chi so so sanh
 * va gap benchmark deu sai theo.
 */
@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dinh dang TT + yyMMdd + 2 chu so
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // NULL khi tuyen con o trang thai DRAFT, chua gan tai xe.
    // Mot tai xe chi duoc gan toi da mot tuyen trong cung ngay (CBR-06)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    // Don vi MET, DA GOM chang depot -> diem dau VA chang diem cuoi -> depot (CBR-01).
    // Chia 1000 khi hien thi ra km
    @Column(name = "total_distance", nullable = false)
    private Double totalDistance;

    @Column(name = "total_weight", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double totalWeight;

    @Column(name = "total_volume", nullable = false, columnDefinition = "DECIMAL(10,3)")
    private Double totalVolume;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "algorithm", nullable = false, length = 30)
    private AlgorithmType algorithm;

    // Ghi nhan thoi gian chay thuat toan de dua vao bang chi so so sanh (BR03-10)
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    // TRUE khi phai dung Haversine vi OSRM loi (CBR-11).
    // Tuyen nay bi loai khoi so lieu trung binh trong bao cao (BR08-03)
    @Column(name = "approximate_distance", nullable = false)
    private Boolean approximateDistance = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private RouteStatus status = RouteStatus.DRAFT;

    // Ghi khi tai xe bam "Xac nhan nhan hang", tuyen chuyen sang IN_PROGRESS
    @Column(name = "departed_at")
    private LocalDateTime departedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Sap theo sequence de tai xe thay dung thu tu giao
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<RouteStop> stops = new ArrayList<>();

    public void addStop(RouteStop stop) {
        stops.add(stop);
        stop.setRoute(this);
    }
}
