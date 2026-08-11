package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Tai xe. Moi tai xe gan voi dung mot tai khoan dang nhap.
 *
 * Mot tai xe chi duoc gan toi da mot tuyen trong cung ngay (CBR-06).
 * Tai xe dang nhap chi thay tuyen cua chinh minh trong ngay hien tai (BR05-01) —
 * rang buoc nay phai kiem o tang service chu khong chi an tren giao dien.
 */
@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Moi tai khoan chi ung voi mot tai xe, nen dat unique tren khoa ngoai
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Dung 10 chu so, bat dau bang 0 (BR01-08)
    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "license_number", length = 30)
    private String licenseNumber;

    // Xe thuong lai. Cho phep NULL vi tai xe moi tao co the chua gan xe nao
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_vehicle_id")
    private Vehicle defaultVehicle;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
