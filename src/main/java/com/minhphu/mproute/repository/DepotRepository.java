package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepotRepository extends JpaRepository<Depot, Long> {

    // Chi mot kho duoc danh dau active (BR01-10), phan tuyen luon xuat phat tu kho nay
    Optional<Depot> findByActiveTrue();
}
