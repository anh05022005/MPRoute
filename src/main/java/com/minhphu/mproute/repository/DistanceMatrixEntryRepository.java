package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.DistanceMatrixEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistanceMatrixEntryRepository extends JpaRepository<DistanceMatrixEntry, Long> {

    // Tra bo nho dem theo dung cap toa do truoc khi goi OSRM (CBR-12)
    Optional<DistanceMatrixEntry> findByFromLatitudeAndFromLongitudeAndToLatitudeAndToLongitude(
            Double fromLatitude, Double fromLongitude, Double toLatitude, Double toLongitude);

    // Nap ca bo cache mot lan de thuat toan khong phai truy van DB trong vong lap.
    // Day la diem mau chot ve hieu nang: bo giai goi ham khoang cach hang chuc nghin lan.
    List<DistanceMatrixEntry> findByFromLatitudeInAndFromLongitudeIn(
            List<Double> latitudes, List<Double> longitudes);
}
