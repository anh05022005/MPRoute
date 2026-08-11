package com.minhphu.mproute.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

/**
 * Mot dong chi tiet cua don hang.
 *
 * CHO Y QUAN TRONG — hoi dong hay hoi dung cho nay:
 * Bang nay CHEP gia tri unitPrice, lineWeight, lineVolume tu danh muc tai thoi diem
 * tao don, KHONG tham chieu toi Product de lay.
 *
 * Ly do: neu chi tham chieu, khi quan tri vien doi gia hoac doi khoi luong thung
 * thi don hang cu se tu doi theo — do la loi nghiep vu nghiem trong (BR02-13).
 * Truong product o day chi de biet da ban san pham nao, khong dung de tinh tien.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "box_count", nullable = false)
    private Integer boxCount;

    // = boxCount * product.weightPerBox tai thoi diem tao don
    @Column(name = "line_weight", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double lineWeight;

    // = boxCount * product.volumePerBox tai thoi diem tao don
    @Column(name = "line_volume", nullable = false, columnDefinition = "DECIMAL(10,3)")
    private Double lineVolume;

    // Chep tu danh muc, doi gia ve sau khong anh huong don cu (BR02-13)
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    // = boxCount * unitPrice
    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;
}
