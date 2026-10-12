package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Mot dong so kho: moi lan so ton cua mot san pham thay doi deu ghi mot dong vao day.
 *
 * <p>BANG NAY CHI GHI THEM, khong sua, khong xoa. Do la diem quan trong nhat can hieu
 * ve ca phan ton kho:
 *
 * <ul>
 *   <li>Sua mot dong cu la pha moi dong sau no — stock_after cua chung tinh tu dong nay,
 *       sua xong thi ca day so phia sau thanh rac.</li>
 *   <li>Xoa mot dong OUT trong khi don hang van o trang thai DELIVERED thi he thong
 *       mat dau tru kho, tuong hang van con, roi cho duyet don khac bang so hang da di mat.</li>
 *   <li>Va xoa cung la vi pham thang CBR-17: dong so kho chinh la du lieu nghiep vu
 *       da phat sinh — no la ly do con so ton hien tai bang con so do.</li>
 * </ul>
 *
 * <p>Ghi sai thi khong bôi di ma ghi mot dong DAO, tro ve dong goc bang reversalOf.
 * Day la nguyen tac but toan dao cua ke toan, khong phai quy uoc rieng cua do an.
 *
 * <p>Cung khuon voi {@link StatusHistory} (CBR-14): mot bang chi ghi them, luu ai lam
 * va luc nao, de sau nay tra lai duoc tung buoc. Khac o cho bang nay tro toi mot bang
 * duy nhat (products) nen dung duoc khoa ngoai that.
 */
@Entity
@Table(
        name = "stock_movements",
        indexes = {
                @Index(name = "idx_stock_movements_product", columnList = "product_id, created_at"),
                @Index(name = "idx_stock_movements_order", columnList = "order_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /*
     * @JdbcTypeCode(VARCHAR) la bat buoc, khong phai tuy chon: thieu dong nay thi
     * Hibernate sinh cot kieu ENUM cua MySQL kem mot CHECK constraint, va ddl-auto=update
     * KHONG sua lai constraint do khi them gia tri enum moi — luc ay moi INSERT deu chet
     * voi "Check constraint is violated" ma khong ro vi sao. Xem docs/02 muc cuoi.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    // Luon duong. Dau nam trong movementType.getSign()
    @Column(name = "box_count", nullable = false)
    private Integer boxCount;

    /*
     * Ton kho SAU dong nay. Tinh lai duoc bang cach cong ca so tu dau, nhung luu san
     * de soat tung buoc: mo so ra la doc duoc ngay dong nao lam so ton sai, khong phai
     * cong tay lai tu dau. Test ledgerSumEqualsStockAfterOfLastRow kiem chinh cot nay.
     */
    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    // Chi co voi OUT va RETURN: don hang nao lam phat sinh dong nay
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // Chi co voi dong dao: tro ve dong bi hoan tac. Dong goc giu nguyen, khong xoa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private StockMovement reversalOf;

    @Column(name = "note", length = 500)
    private String note;

    // NULL khi he thong tu ghi (OUT, RETURN, ton dau ky) chu khong phai nguoi dung bam nut
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** So thung co dau, dung khi cong lai ca so de doi chieu voi stock_boxes. */
    @Transient
    public int getSignedBoxCount() {
        return movementType.getSign() * boxCount;
    }
}
