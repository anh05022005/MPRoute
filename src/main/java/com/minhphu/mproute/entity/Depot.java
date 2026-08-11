package com.minhphu.mproute.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kho hang. Dung tu "depot" vi day la thuat ngu chuan cua bai toan VRP,
 * trung voi ky hieu c0 trong docs/03-thuat-toan.md va voi tai lieu CVRPLIB.
 *
 * Moi tuyen deu xuat phat tu depot va ket thuc tai depot (CBR-01).
 * Chi mot ban ghi duoc danh dau active = true (BR01-10).
 */
@Entity
@Table(name = "depots")
@Getter
@Setter
@NoArgsConstructor
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    // 7 chu so thap phan tuong duong do chinh xac khoang 1 cm
    @Column(name = "latitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(name = "longitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double longitude;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
