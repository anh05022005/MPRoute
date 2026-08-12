package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    boolean existsByCode(String code);

    // Man hinh tao don chi cho chon san pham dang su dung
    List<Product> findByStatus(RecordStatus status);

    // Man hinh danh muc co tim kiem va phan trang (CN-01-01)
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * Tim kiem tren man hinh danh muc: go tu khoa khop ca ma lan ten, loc them trang thai.
     * Ca hai dieu kien deu bo qua duoc bang cach truyen null.
     *
     * Dung tham so :keyword va :status chu KHONG noi chuoi SQL — noi chuoi la mo duong
     * cho tan cong SQL injection (xem muc Bao mat trong docs/10-quy-uoc-ky-thuat.md).
     */
    @Query("""
            SELECT p FROM Product p
            WHERE (:keyword IS NULL
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("status") RecordStatus status,
                         Pageable pageable);
}
