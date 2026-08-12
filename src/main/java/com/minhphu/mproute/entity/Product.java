package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * San pham trong danh muc.
 *
 * weightPerBox va volumePerBox la DAU VAO CUA THUAT TOAN, bat buoc lon hon 0 (BR01-01).
 * Thieu hai truong nay thi khong chay duoc phan tuyen.
 *
 * Su tuong phan giua hai truong nay chinh la ly do bai toan can HAI rang buoc suc chua:
 * bong tay trang 6 kg nhung 0.085 m3 (nhe, cong kenh), nuoc lau san 24.8 kg nhung
 * 0.031 m3 (nang, gon). Xe cho toan bong tay trang se day cho truoc khi day tai.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // Chuoi nay hien thang ra giao dien nen viet tieng Viet co dau (docs/08-du-lieu-mau.md)
    @Column(name = "unit", nullable = false, length = 20)
    private String unit = "Thùng";

    // kg moi thung — dau vao rang buoc khoi luong (CBR-03)
    @Column(name = "weight_per_box", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double weightPerBox;

    // m3 moi thung — dau vao rang buoc the tich (CBR-04)
    @Column(name = "volume_per_box", nullable = false, columnDefinition = "DECIMAL(10,3)")
    private Double volumePerBox;

    // Tien dung BigDecimal, khong bao gio dung double
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;

    @Column(name = "note", length = 500)
    private String note;
}
