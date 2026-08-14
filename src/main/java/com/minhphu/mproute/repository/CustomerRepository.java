package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByPhone(String phone);

    // Man hinh tao don chi cho chon khach dang su dung
    List<Customer> findByStatus(RecordStatus status);

    Page<Customer> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * Man hinh danh muc: tim theo ten cua hang, nguoi lien he hoac so dien thoai,
     * loc them trang thai (CN-01-04). Truyen null de bo qua dieu kien.
     *
     * Dung tham so co ten, khong noi chuoi SQL.
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE (:keyword IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR c.phone LIKE CONCAT('%', :keyword, '%'))
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<Customer> search(@Param("keyword") String keyword,
                          @Param("status") RecordStatus status,
                          Pageable pageable);
}
