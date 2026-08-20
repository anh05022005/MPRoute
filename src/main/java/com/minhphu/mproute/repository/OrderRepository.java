package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCode(String code);

    /**
     * Truy van nong nhat cua he thong, chay moi lan phan tuyen.
     * Chi lay don APPROVED co ngay giao trung ngay duoc chon (BR03-01).
     * Index idx_orders_date_status phuc vu dung truy van nay.
     */
    List<Order> findByDeliveryDateAndStatus(LocalDate deliveryDate, OrderStatus status);

    Page<Order> findByDeliveryDate(LocalDate deliveryDate, Pageable pageable);

    /**
     * Man hinh danh sach don: loc theo ngay giao, trang thai, khach hang (CN-02-01).
     * Truyen null de bo qua tung dieu kien.
     *
     * JOIN FETCH customer vi danh sach nao cung hien ten khach: khong nap san thi
     * moi dong sinh them mot cau SELECT rieng (van de N+1), 15 dong la 16 cau truy van.
     *
     * Dung tham so co ten, khong noi chuoi SQL.
     */
    @Query(value = """
            SELECT o FROM Order o
            JOIN FETCH o.customer c
            WHERE (:deliveryDate IS NULL OR o.deliveryDate = :deliveryDate)
              AND (:status IS NULL OR o.status = :status)
              AND (:customerId IS NULL OR c.id = :customerId)
            """,
            // Phai viet tay cau dem: Spring Data khong tu suy ra duoc cau dem
            // tu mot cau co JOIN FETCH
            countQuery = """
            SELECT COUNT(o) FROM Order o
            WHERE (:deliveryDate IS NULL OR o.deliveryDate = :deliveryDate)
              AND (:status IS NULL OR o.status = :status)
              AND (:customerId IS NULL OR o.customer.id = :customerId)
            """)
    Page<Order> search(@Param("deliveryDate") LocalDate deliveryDate,
                       @Param("status") OrderStatus status,
                       @Param("customerId") Long customerId,
                       Pageable pageable);

    // Tra cuu cong khai theo SDT, chi hien don trong 30 ngay gan nhat (BR07-03)
    @Query("SELECT o FROM Order o WHERE o.customer.phone = :phone "
         + "AND o.deliveryDate >= :fromDate ORDER BY o.deliveryDate DESC")
    List<Order> findRecentByCustomerPhone(@Param("phone") String phone,
                                          @Param("fromDate") LocalDate fromDate);

    // Dem so don da tao trong ngay de sinh 4 chu so thu tu cua ma don (BR02-01)
    long countByDeliveryDate(LocalDate deliveryDate);
}
