package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Phuong tien giao hang.
 *
 * maxWeight va maxVolume la HAI RANG BUOC CUA THUAT TOAN, bat buoc lon hon 0 (BR01-03).
 * Thuat toan phai kiem CA HAI dong thoi truoc khi them mot diem vao tuyen,
 * chi kiem mot cai la sai nghiep vu (CBR-03, CBR-04).
 *
 * Chi xe AVAILABLE moi duoc dua vao phan tuyen (CBR-05). Trang thai IN_USE do
 * he thong tu dat khi chot tuyen, quan tri vien khong chon tay duoc (BR01-04).
 */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    // Tai trong toi da, don vi kg
    @Column(name = "max_weight", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double maxWeight;

    // The tich thung xe, don vi m3
    @Column(name = "max_volume", nullable = false, columnDefinition = "DECIMAL(10,3)")
    private Double maxVolume;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "note", length = 500)
    private String note;
}
