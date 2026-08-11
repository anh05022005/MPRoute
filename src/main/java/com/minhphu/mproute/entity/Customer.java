package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.PaymentMethod;
import com.minhphu.mproute.entity.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Khach hang — cua hang tap hoa, dai ly, sieu thi nho trong noi thanh Ha Noi.
 *
 * Khach dat hang QUA DIEN THOAI, khong dang nhap he thong. Day la chu y thiet ke:
 * khach quen goi dien de vua dat vua hoi gia, bat ho dang nhap web se khien
 * he thong khong ai dung.
 *
 * Toa do BAT BUOC co truoc khi phan tuyen (CBR-07). Don thieu toa do bi loai
 * khoi danh sach cho va hien canh bao.
 */
@Entity
@Table(
        name = "customers",
        indexes = @Index(name = "idx_customers_phone", columnList = "phone")
)
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    // Danh index vi trang tra cuu cong khai cho phep tim don theo so dien thoai
    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    // Dung de gui thong bao ETA sau khi chot tuyen (BR04-11)
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(name = "longitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double longitude;

    // Gan co dinh voi khach, nhung ghi de duoc cho rieng tung don (BR02-15)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    // Van ban tu do: "Chi nhan buoi sang", "Xe lon khong vao duoc ngo"
    @Column(name = "delivery_note", length = 500)
    private String deliveryNote;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
