package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    // Man hinh phan tuyen chi liet ke xe AVAILABLE de dieu phoi tich chon (CBR-05, BR03-02)
    List<Vehicle> findByStatus(VehicleStatus status);
}
