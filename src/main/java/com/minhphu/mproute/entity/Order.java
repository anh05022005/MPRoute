package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.PaymentMethod;
import com.minhphu.mproute.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Don hang. Khach dat qua dien thoai, dieu phoi vien nhap vao he thong.
 *
 * Bang nay dat ten "orders" so nhieu vi ORDER la tu khoa SQL.
 *
 * totalWeight va totalVolume do HE THONG TU TINH tu cac dong chi tiet,
 * khong cho sua tay (BR02-04). Hai truong nay chinh la nhu cau dau vao
 * cua thuat toan phan tuyen.
 */
@Entity
@Table(
        name = "orders",
        indexes = @Index(name = "idx_orders_date_status", columnList = "delivery_date, status")
)
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dinh dang MP + yyMMdd + 4 chu so thu tu trong ngay (BR02-01)
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    // Chep tu khach hang luc luu don, khong doc thang tu Customer: sua danh muc sau nay
    // thi don da giao khong bi doi theo (BR02-07)
    @Column(name = "delivery_address", nullable = false, length = 255)
    private String deliveryAddress;

    // Cung chep tu khach hang nhu dia chi (BR02-07)
    @Column(name = "latitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(name = "longitude", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double longitude;

    // He thong tu tinh tu cac dong chi tiet, khong cho sua tay (BR02-04)
    @Column(name = "total_weight", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double totalWeight;

    @Column(name = "total_volume", nullable = false, columnDefinition = "DECIMAL(10,3)")
    private Double totalVolume;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Dien san theo khach hang, sua duoc cho rieng don, khong doi danh muc (BR02-15)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    // Bang totalAmount neu CASH, bang 0 neu CREDIT hoac PREPAID (BR02-14).
    // Doi hinh thuc thanh toan thi phai tinh lai truong nay ngay (BR02-16)
    @Column(name = "amount_to_collect", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountToCollect = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_APPROVAL;

    // Huy don bat buoc nhap ly do, khong xoa khoi DB (BR02-12)
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Cac dong chi tiet. cascade + orphanRemoval de man hinh tao don them/xoa dong
    // roi luu mot lan la xong, khong phai goi repository rieng cho tung dong.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Giu hai chieu quan he dong bo, tranh truong hop luu xong item khong co order_id
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
