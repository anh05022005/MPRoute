package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    // Tai xe dang nhap: tra ho so tai xe tu tai khoan de loc tuyen cua chinh ho (BR05-01)
    Optional<Driver> findByUserId(Long userId);

    List<Driver> findByStatus(RecordStatus status);
}
