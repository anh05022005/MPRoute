package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.FailureReason;
import com.minhphu.mproute.entity.enums.StopStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mot diem dung trong tuyen, ung voi dung mot don hang.
 *
 * Khoa ngoai order_id dat UNIQUE de bao dam mot don chi thuoc dung mot tuyen (CBR-02).
 * Rang buoc nay o muc co so du lieu, khong phu thuoc vao code kiem tra.
 */
@Entity
@Table(
        name = "route_stops",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_route_sequence",
                columnNames = {"route_id", "sequence"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Route route;

    // unique = true: mot don chi nam trong dung mot tuyen (CBR-02)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // Bat dau tu 1. Tai xe khong doi duoc thu tu nay (BR05-02)
    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    // Quang duong tu diem truoc toi diem nay, don vi MET.
    // Diem dau tien thi la khoang cach tu depot toi day
    @Column(name = "leg_distance", nullable = false)
    private Double legDistance;

    // ETA tinh theo CBR-13: gio xuat phat + quang duong tich luy / toc do + so diem truoc * thoi gian boc do
    @Column(name = "estimated_arrival")
    private LocalDateTime estimatedArrival;

    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;

    // Toa do GPS luc tai xe bam nut, neu trinh duyet cho phep (BR05-08)
    @Column(name = "updated_latitude", columnDefinition = "DECIMAL(10,7)")
    private Double updatedLatitude;

    @Column(name = "updated_longitude", columnDefinition = "DECIMAL(10,7)")
    private Double updatedLongitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private StopStatus status = StopStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "failure_reason", length = 50)
    private FailureReason failureReason;

    // Bat buoc nhap khi chon ly do OTHER (BR05-06)
    @Column(name = "failure_note", length = 500)
    private String failureNote;

    @Column(name = "collected_amount", precision = 15, scale = 2)
    private BigDecimal collectedAmount = BigDecimal.ZERO;

    // Bat buoc ghi khi thu khac so tien phai thu (BR05-12)
    @Column(name = "collection_note", length = 500)
    private String collectionNote;
}
