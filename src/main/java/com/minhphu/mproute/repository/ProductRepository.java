package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    boolean existsByCode(String code);

    // Man hinh tao don chi cho chon san pham dang su dung
    List<Product> findByStatus(RecordStatus status);

    // Man hinh danh muc co tim kiem va phan trang (CN-01-01)
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
