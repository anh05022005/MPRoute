package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.AlgorithmType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Chi so cua mot lan chay thuat toan phan tuyen.
 *
 * BANG QUAN TRONG NHAT CHO BAO CAO. Chi so duoc luu TAI THOI DIEM CHAY va
 * khong bao gio tinh lai ve sau (CBR-18) — vi khoang cach, danh muc xe, du lieu don
 * deu co the doi, tinh lai se ra con so khac han.
 *
 * Hai moc so sanh la diem mau chot cua chuong 4 bao cao:
 *  - Moc A: moi don mot chuyen khu hoi tu depot. Mo phong quy trinh hien tai
 *    cua doanh nghiep. Cho con so giam dep nhung de bi be la "tu dung moc kem roi so".
 *  - Moc B: giu nguyen cach chia xe cua thuat toan, chi doi thu tu giao theo
 *    thu tu nhan don. Do RIENG dong gop cua viec sap thu tu. Con so khiem ton hon
 *    nhung dang tin hon han.
 *
 * Luon bao cao CA HAI. Chi tiet o docs/07-thuc-nghiem.md.
 */
@Entity
@Table(name = "optimization_results")
@Getter
@Setter
@NoArgsConstructor
public class OptimizationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "route_count", nullable = false)
    private Integer routeCount;

    @Column(name = "vehicle_count", nullable = false)
    private Integer vehicleCount;

    // Tat ca quang duong luu bang MET
    @Column(name = "baseline_a_distance", nullable = false)
    private Double baselineADistance;

    @Column(name = "baseline_b_distance", nullable = false)
    private Double baselineBDistance;

    @Column(name = "optimized_distance", nullable = false)
    private Double optimizedDistance;

    @Column(name = "reduction_a_meters")
    private Double reductionAMeters;

    @Column(name = "reduction_a_percent")
    private Double reductionAPercent;

    @Column(name = "reduction_b_meters")
    private Double reductionBMeters;

    @Column(name = "reduction_b_percent")
    private Double reductionBPercent;

    // Trung binh phan tram su dung tai trong va the tich cua cac xe trong lan chay
    @Column(name = "weight_utilization")
    private Double weightUtilization;

    @Column(name = "volume_utilization")
    private Double volumeUtilization;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "algorithm", nullable = false, length = 30)
    private AlgorithmType algorithm;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    // Lan chay dung Haversine thi hien rieng, khong tinh vao trung binh (BR08-03)
    @Column(name = "approximate_distance", nullable = false)
    private Boolean approximateDistance = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
