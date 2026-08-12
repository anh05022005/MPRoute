package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    // Man hinh phan tuyen chi liet ke xe AVAILABLE de dieu phoi tich chon (CBR-05, BR03-02)
    List<Vehicle> findByStatus(VehicleStatus status);

    /**
     * Man hinh danh muc: tim theo bien so hoac loai xe, loc them trang thai (CN-01-02).
     * Truyen null de bo qua dieu kien. Dung tham so, khong noi chuoi SQL.
     */
    @Query("""
            SELECT v FROM Vehicle v
            WHERE (:keyword IS NULL
                   OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.vehicleType) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR v.status = :status)
            """)
    Page<Vehicle> search(@Param("keyword") String keyword,
                         @Param("status") VehicleStatus status,
                         Pageable pageable);
}
