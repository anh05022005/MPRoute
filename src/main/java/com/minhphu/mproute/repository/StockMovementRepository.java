package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.StockMovement;
import com.minhphu.mproute.entity.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** So kho cua mot san pham, moi nhat len dau. */
    List<StockMovement> findByProductIdOrderByCreatedAtDescIdDesc(Long productId);

    /** Cac dong sinh ra boi mot don hang — dung khi can biet don da tru kho chua. */
    List<StockMovement> findByOrderId(Long orderId);

    boolean existsByOrderIdAndMovementType(Long orderId, StockMovementType movementType);

    /** Dong da bi hoan tac roi thi khong cho hoan tac lan hai. */
    boolean existsByReversalOfId(Long reversalOfId);

    /**
     * San pham da tung co dong so kho nao chua.
     *
     * Dung de nhan ra san pham chua he duoc theo doi ton kho — luc do CatalogSeeder ghi
     * cho no mot dong ton dau ky. Dieu kien nay dung ca tren may dung moi lan dau lan tren
     * may da co san du lieu tu truoc khi co phan ton kho.
     */
    boolean existsByProductId(Long productId);

    /**
     * Tong so thung CO DAU cua mot san pham — cong ca so kho tu dong dau tien.
     *
     * Dung de doi chieu voi products.stock_boxes: hai con so lech nhau la co loi,
     * va khi hoi dong hoi "sao em biet so ton nay dung" thi day la cau tra loi.
     *
     * COALESCE de san pham chua co dong nao tra ve 0 thay vi null.
     */
    @Query("""
            SELECT COALESCE(SUM(
                CASE
                    WHEN m.movementType IN (com.minhphu.mproute.entity.enums.StockMovementType.OUT,
                                            com.minhphu.mproute.entity.enums.StockMovementType.ADJUST_DOWN)
                    THEN -m.boxCount
                    ELSE m.boxCount
                END), 0)
            FROM StockMovement m
            WHERE m.product.id = :productId
            """)
    int sumSignedBoxCount(@Param("productId") Long productId);

    /**
     * So kho cho man hinh tra cuu: loc theo san pham, loai phat sinh va khoang ngay,
     * bo qua dieu kien nao bang cach truyen null.
     *
     * Dung tham so co ten chu KHONG noi chuoi SQL (muc Bao mat, docs/10).
     */
    @Query("""
            SELECT m FROM StockMovement m
            WHERE (:productId IS NULL OR m.product.id = :productId)
              AND (:movementType IS NULL OR m.movementType = :movementType)
              AND (:from IS NULL OR m.createdAt >= :from)
              AND (:to IS NULL OR m.createdAt <= :to)
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    Page<StockMovement> search(@Param("productId") Long productId,
                               @Param("movementType") StockMovementType movementType,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               Pageable pageable);

    /** Ban khong phan trang cua search(), dung cho xuat Excel so kho. */
    @Query("""
            SELECT m FROM StockMovement m
            WHERE (:productId IS NULL OR m.product.id = :productId)
              AND (:movementType IS NULL OR m.movementType = :movementType)
              AND (:from IS NULL OR m.createdAt >= :from)
              AND (:to IS NULL OR m.createdAt <= :to)
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<StockMovement> searchAll(@Param("productId") Long productId,
                                  @Param("movementType") StockMovementType movementType,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
