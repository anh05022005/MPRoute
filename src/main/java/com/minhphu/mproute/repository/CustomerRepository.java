package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByPhone(String phone);

    // Man hinh tao don chi cho chon khach dang su dung
    List<Customer> findByStatus(RecordStatus status);

    Page<Customer> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
