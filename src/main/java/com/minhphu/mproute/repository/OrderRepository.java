package com.minhphu.mproute.repository;

import com.minhphu.mproute.dto.DailyOrderCount;
import com.minhphu.mproute.dto.OrderStatusCount;
import com.minhphu.mproute.dto.ProductBoxCount;
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

    /**
     * Don dang giu hang trong kho, NAP SAN ca cac dong chi tiet — dung khi khoi tao ton kho
     * lan dau (StockOpeningSeeder).
     *
     * Phai JOIN FETCH chu khong dung findByStatusIn thuong: StockOpeningSeeder la
     * CommandLineRunner nen khong co session mo san, doc order.getItems() tren thuc the
     * da roi session se nem LazyInitializationException. Nap san o day gon hon la mo mot
     * giao dich dai bao ca vong lap khoi tao.
     *
     * DISTINCT vi JOIN FETCH mot collection lam moi don lap lai dung bang so dong chi tiet.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.status IN :statuses
            """)
    List<Order> findByStatusInWithItems(@Param("statuses") List<OrderStatus> statuses);

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

    // ==================== Nguon du lieu cho bao cao nhom 8 ====================

    /**
     * So don, tong kg, tong m3 va tong tien theo TUNG NGAY GIAO — bao cao RPT-01 (CN-08-01).
     *
     * Gop san trong CSDL roi moi tra ve, khong nap het don ra rai roi dem trong Java: mot
     * khoang 365 ngay cua Minh Phu la vai nghin don, nap het ra chi de dem la thua.
     *
     * BO DON DA HUY (BR08-04): don huy khong phat sinh chuyen giao nao, dem vao thi so don
     * cua bao cao khong khop voi so hang thuc su ra khoi kho.
     */
    @Query("""
            SELECT new com.minhphu.mproute.dto.DailyOrderCount(
                       o.deliveryDate, COUNT(o), SUM(o.totalWeight), SUM(o.totalVolume), SUM(o.totalAmount))
            FROM Order o
            WHERE o.deliveryDate BETWEEN :fromDate AND :toDate
              AND o.status <> com.minhphu.mproute.entity.enums.OrderStatus.CANCELLED
            GROUP BY o.deliveryDate
            ORDER BY o.deliveryDate
            """)
    List<DailyOrderCount> countOrdersByDay(@Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate);

    /**
     * So thung da giao theo tung san pham trong ky (CN-08-01).
     *
     * COUNT(DISTINCT o.id) chu khong COUNT(o): mot don co the co hai dong cung mot san pham,
     * dem thang la mot don bi tinh hai lan o cot "so don co mat".
     */
    @Query("""
            SELECT new com.minhphu.mproute.dto.ProductBoxCount(
                       p.code, p.name, SUM(i.boxCount), SUM(i.lineWeight), SUM(i.lineVolume),
                       COUNT(DISTINCT o.id))
            FROM OrderItem i
            JOIN i.order o
            JOIN i.product p
            WHERE o.deliveryDate BETWEEN :fromDate AND :toDate
              AND o.status <> com.minhphu.mproute.entity.enums.OrderStatus.CANCELLED
            GROUP BY p.code, p.name
            ORDER BY SUM(i.boxCount) DESC
            """)
    List<ProductBoxCount> countBoxesByProduct(@Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);

    /**
     * So don theo tung trang thai — bao cao RPT-02 (CN-08-02).
     *
     * Giu ca don CANCELLED o day, khac voi hai cau tren: RPT-02 do ty le giao thanh cong,
     * ma mau so cua no la "don DA PHAN TUYEN" (BR08-06). Don huy bi loai o tang service khi
     * tinh ty le, nhung van hien ra bang de nguoi xem biet ky do co bao nhieu don bi huy.
     */
    @Query("""
            SELECT new com.minhphu.mproute.dto.OrderStatusCount(o.status, COUNT(o))
            FROM Order o
            WHERE o.deliveryDate BETWEEN :fromDate AND :toDate
            GROUP BY o.status
            """)
    List<OrderStatusCount> countOrdersByStatus(@Param("fromDate") LocalDate fromDate,
                                               @Param("toDate") LocalDate toDate);

}
