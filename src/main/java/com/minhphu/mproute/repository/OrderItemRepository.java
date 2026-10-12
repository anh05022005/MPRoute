package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * So thung DA CAM KET cua tung san pham: tong so thung thuoc cac don da duyet
     * nhung chua giao xong.
     *
     * Con so nay KHONG luu o dau ca, moi lan can thi dem lai. Nho vay no khong bao gio
     * lech khoi bang orders — khac voi viec luu them mot cot "da cam ket" roi phai nho
     * cap nhat o moi cho chuyen trang thai don.
     *
     * Ba trang thai duoc tinh la cam ket:
     *   APPROVED    — da tru kho luc duyet, hang dang cho xep tuyen
     *   ROUTED      — da xep vao tuyen, chua xuat phat
     *   DELIVERING  — dang tren xe, chua biet giao duoc hay khong
     * Den DELIVERED thi hang da di han, den CANCELLED / FAILED / POSTPONED thi da hoan
     * kho roi, nen ca hai nhom deu khong con la cam ket.
     *
     * Tra ve cap (productId, soThung) duoi dang Object[] — chi dung o man hinh kho va
     * form don hang, KHONG goi trong vong lap thuat toan.
     */
    @Query("""
            SELECT i.product.id, SUM(i.boxCount)
            FROM OrderItem i
            WHERE i.order.status IN (com.minhphu.mproute.entity.enums.OrderStatus.APPROVED,
                                     com.minhphu.mproute.entity.enums.OrderStatus.ROUTED,
                                     com.minhphu.mproute.entity.enums.OrderStatus.DELIVERING)
            GROUP BY i.product.id
            """)
    List<Object[]> sumCommittedBoxesByProduct();
}
