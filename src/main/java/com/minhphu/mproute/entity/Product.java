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

    /*
     * So thung CON RANH de nhan don moi — KHONG phai so thung dang nam trong kho.
     *
     * Hai con so nay khac nhau: hang da duyet cho mot don thi van nam trong kho cho
     * tai xe den lay, nhung khong duoc hua cho khach khac nua. Quan he:
     *     trong kho = con ranh + da cam ket
     * trong do "da cam ket" KHONG luu o dau ca, no dem duoc tu order_items cua cac don
     * dang APPROVED / ROUTED / DELIVERING. So nao suy ra duoc thi khong luu, vi luu la
     * them mot cho co the lech.
     *
     * Khong co man hinh nao go truc tiep vao cot nay. No chi doi khi StockService ghi
     * mot dong vao stock_movements — co lam the thi cong ca so moi luon khop con so nay.
     *
     * columnDefinition co DEFAULT 0 de ddl-auto=update them duoc cot NOT NULL vao bang
     * products dang co san du lieu ma khong chet luc khoi dong.
     */
    @Column(name = "stock_boxes", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer stockBoxes = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;

    @Column(name = "note", length = 500)
    private String note;
}
