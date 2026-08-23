package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.VehicleForm;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiep vu danh muc phuong tien (CN-01-02).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public Page<Vehicle> search(String keyword, VehicleStatus status, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return vehicleRepository.search(normalizedKeyword, status, pageable);
    }

    @Transactional(readOnly = true)
    public Vehicle getById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phương tiện"));
    }

    // BR01-03: bien so khong duoc trung. excludeId de xe tu giu bien so cua chinh no khi sua
    @Transactional(readOnly = true)
    public boolean isLicensePlateTaken(String licensePlate, Long excludeId) {
        return vehicleRepository.findByLicensePlate(licensePlate)
                .filter(vehicle -> !vehicle.getId().equals(excludeId))
                .isPresent();
    }

    @Transactional
    public Vehicle save(VehicleForm form) {
        Vehicle vehicle = (form.getId() == null) ? new Vehicle() : getById(form.getId());

        vehicle.setLicensePlate(form.getLicensePlate().trim());
        vehicle.setVehicleType(form.getVehicleType().trim());
        vehicle.setMaxWeight(form.getMaxWeight());
        vehicle.setMaxVolume(form.getMaxVolume());
        vehicle.setNote(form.getNote());

        /*
         * BR01-04: xe DANG CHAY thi giu nguyen trang thai, khong cho form ghi de.
         *
         * Vi sao phai chan o day chu khong chi an o chon tren giao dien: xe dang cho hang
         * chay ngoai duong ma bi sua thanh "San sang" thi lan phan tuyen tiep theo he thong
         * se xep them don cho no, thanh ra mot xe hai tuyen cung luc.
         * Trang thai IN_USE do he thong dat khi chot tuyen va go ra khi tuyen hoan thanh.
         */
        if (vehicle.getStatus() != VehicleStatus.IN_USE) {
            vehicle.setStatus(form.getStatus());
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Luu phuong tien {} - {}", saved.getLicensePlate(), saved.getVehicleType());
        return saved;
    }

    /**
     * Doi giua SAN SANG va BAO TRI.
     *
     * Khong co ham xoa cung (CBR-17): xe da chay tuyen cu ma xoa di thi bao cao cu
     * mat thong tin xe nao chay tuyen nao.
     *
     * @throws IllegalStateException khi xe dang chay tuyen
     */
    @Transactional
    public VehicleStatus toggleMaintenance(Long id) {
        Vehicle vehicle = getById(id);

        if (vehicle.getStatus() == VehicleStatus.IN_USE) {
            throw new IllegalStateException(
                    "Xe " + vehicle.getLicensePlate() + " đang chạy tuyến, không đổi trạng thái được.");
        }

        VehicleStatus newStatus = (vehicle.getStatus() == VehicleStatus.AVAILABLE)
                ? VehicleStatus.MAINTENANCE
                : VehicleStatus.AVAILABLE;
        vehicle.setStatus(newStatus);
        vehicleRepository.save(vehicle);
        log.info("Phuong tien {} chuyen sang trang thai {}", vehicle.getLicensePlate(), newStatus);
        return newStatus;
    }
}
