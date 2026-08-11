package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.DistanceSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Mot o trong ma tran khoang cach, dong vai tro bo nho dem.
 *
 * Luu theo TOA DO chu khong theo id thuc the, vi diem dau va diem cuoi co the la
 * kho hoac khach hang — hai loai thuc the khac nhau, luu theo toa do thi thong nhat.
 *
 * Bang nay la bao hiem khi demo: hom bao ve OSRM khong chay duoc thi du lieu
 * da cache van cho he thong hoat dong binh thuong.
 */
@Entity
@Table(
        name = "distance_matrix",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coordinate_pair",
                columnNames = {"from_latitude", "from_longitude", "to_latitude", "to_longitude"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class DistanceMatrixEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_latitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double fromLatitude;

    @Column(name = "from_longitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double fromLongitude;

    @Column(name = "to_latitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double toLatitude;

    @Column(name = "to_longitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double toLongitude;

    // Luu bang MET de tranh sai so cong don khi tinh tong nhieu chang
    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    // OSRM tra ve ca thoi gian di chuyen, luu lai de dung sau neu can
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source", nullable = false, length = 20)
    private DistanceSource source;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
