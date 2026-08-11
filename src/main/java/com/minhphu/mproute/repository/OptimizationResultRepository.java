package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.OptimizationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, Long> {

    /**
     * Nguon du lieu cho bao cao RPT-03 — bao cao hieu qua toi uu,
     * la bao cao quan trong nhat cua do an (CN-08-03).
     * Lay tu chi so da luu san, KHONG tinh lai (CBR-18, BR08-02).
     */
    List<OptimizationResult> findByDeliveryDateBetweenOrderByDeliveryDateAsc(
            LocalDate fromDate, LocalDate toDate);

    // Tuyen dung xap xi hien rieng, khong tinh vao trung binh (BR08-03)
    List<OptimizationResult> findByDeliveryDateBetweenAndApproximateDistanceFalse(
            LocalDate fromDate, LocalDate toDate);

    List<OptimizationResult> findByDeliveryDate(LocalDate deliveryDate);
}
