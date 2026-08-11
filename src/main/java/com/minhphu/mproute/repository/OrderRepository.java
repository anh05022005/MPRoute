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

    // Tra cuu cong khai theo SDT, chi hien don trong 30 ngay gan nhat (BR07-03)
    @Query("SELECT o FROM Order o WHERE o.customer.phone = :phone "
         + "AND o.deliveryDate >= :fromDate ORDER BY o.deliveryDate DESC")
    List<Order> findRecentByCustomerPhone(@Param("phone") String phone,
                                          @Param("fromDate") LocalDate fromDate);

    // Dem so don da tao trong ngay de sinh 4 chu so thu tu cua ma don (BR02-01)
    long countByDeliveryDate(LocalDate deliveryDate);
}
