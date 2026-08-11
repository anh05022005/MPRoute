package com.minhphu.mproute.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tham so cau hinh he thong, luu dang cap khoa - gia tri.
 *
 * Cac tham so anh huong truc tiep toi thuat toan: AVERAGE_SPEED, LOADING_TIME,
 * DEPARTURE_TIME, HAVERSINE_FACTOR, DEFAULT_ALGORITHM.
 * Doi AVERAGE_SPEED roi chay lai phan tuyen thi ETA phai doi theo — day la cach
 * kiem chung tham so thuc su duoc dung chu khong bi hard-code (CN-01-08).
 */
@Entity
@Table(name = "system_parameters")
@Getter
@Setter
@NoArgsConstructor
public class SystemParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    // Luu dang chuoi cho moi loai tham so, tang goi tu doi sang so khi dung
    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "description", length = 500)
    private String description;
}
