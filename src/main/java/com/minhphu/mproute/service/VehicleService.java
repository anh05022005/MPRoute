package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.VehicleForm;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nghiep vu danh muc phuong tien (CN-01-02) va luat xe nao duoc dung vao ngay nao (CBR-05).
 *
 * HAI CAU HOI KHAC NHAU, truoc day bi gop chung vao mot cot status (docs/09 muc B6):
 *   - "Xe da co viec NGAY D chua?" — tra theo bang routes cua dung ngay D. Dung khi phan tuyen.
 *   - "Xe dang o ngoai duong khong?" — cot status = IN_USE, bat khi tai xe nhan hang, tat khi
 *     tuyen xong hoac bi huy. Dung khi dieu xe cuu ho ngay luc nay.
 * Gop lam mot thi chot ke hoach ngay 29 vao ngay 28 la ca doi xe "Dang chay" tu ngay 28,
 * khong phan tuyen duoc ngay 30 va khong con xe nao di cuu ho trong ngay 28.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    /*
     * Tuyen o mot trong ba trang thai nay nghia la xe DA CO VIEC ngay do. Nhap chua tinh vi
     * chay lai phan tuyen la xoa nhap (BR03-09). Hoan thanh thi xe da ve kho, chay them chuyen
     * thu hai trong ngay van duoc — giong het truoc day khi xe ve AVAILABLE luc xong tuyen.
     */
    private static final List<RouteStatus> STATUSES_HOLDING_VEHICLE =
            List.of(RouteStatus.CONFIRMED, RouteStatus.IN_PROGRESS, RouteStatus.INCIDENT);

    // Tuyen o hai trang thai nay nghia la xe dang o ngoai duong
    private static final List<RouteStatus> STATUSES_ON_THE_ROAD =
            List.of(RouteStatus.IN_PROGRESS, RouteStatus.INCIDENT);

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;

    /**
     * Xe dua vao phan tuyen duoc cho ngay D (CBR-05, BR03-02).
     *
     * Khong loc theo status = AVAILABLE nua: xe dang chay tuyen hom nay (IN_USE) van phan
     * tuyen duoc cho ngay mai. Chi loai xe bao tri va xe da co tuyen chot trong chinh ngay D.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> findSchedulableOn(LocalDate deliveryDate) {
        Set<Long> busyVehicleIds = new HashSet<>(
                routeRepository.findVehicleIdsByDeliveryDateAndStatusIn(deliveryDate, STATUSES_HOLDING_VEHICLE));
        return vehicleRepository.findAll(Sort.by("id")).stream()
                .filter(vehicle -> vehicle.getStatus() != VehicleStatus.MAINTENANCE)
                .filter(vehicle -> !busyVehicleIds.contains(vehicle.getId()))
                .toList();
    }

    /**
     * Kiem lai o may chu mot xe dieu phoi vien da tich chon — khong tin danh sach o trinh duyet,
     * hoi dong se gui thang id cua mot xe dang ban len xem he thong co nhan khong (CBR-15).
     *
     * @throws IllegalStateException kem ly do cu the de hien len man hinh phan tuyen
     */
    @Transactional(readOnly = true)
    public void checkSchedulableOn(Vehicle vehicle, LocalDate deliveryDate) {
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new IllegalStateException(String.format(
                    "Xe %s đang bảo trì nên không đưa vào phân tuyến được.", vehicle.getLicensePlate()));
        }
        if (routeRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(
                vehicle.getId(), deliveryDate, STATUSES_HOLDING_VEHICLE)) {
            throw new IllegalStateException(String.format(
                    "Xe %s đã có tuyến đã chốt trong ngày này nên không đưa vào phân tuyến được.",
                    vehicle.getLicensePlate()));
        }
    }

    /**
     * Xe dieu di cuu ho NGAY LUC NAY (BR06-06): phai dang dung o kho — khong o ngoai duong,
     * khong bao tri — VA chua co tuyen nao trong ngay xay ra su co. Xe da chot cho ngay mai
     * thi van di cuu ho hom nay duoc.
     */
    @Transactional(readOnly = true)
    public boolean isAtDepotAndFreeOn(Vehicle vehicle, LocalDate deliveryDate) {
        return vehicle.getStatus() == VehicleStatus.AVAILABLE
                && !routeRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(
                        vehicle.getId(), deliveryDate, STATUSES_HOLDING_VEHICLE);
    }

    /** Tai xe bam nhan hang: xe roi kho, tu luc nay moi la "Dang chay" (BR05-03). */
    @Transactional
    public void markOnTheRoad(Vehicle vehicle) {
        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.setStatus(VehicleStatus.IN_USE);
            vehicleRepository.save(vehicle);
        }
    }

    /**
     * Tuyen xong hoac bi huy: tra xe ve San sang (BR04-09, BR05-09).
     *
     * Chi tra khi xe KHONG con tuyen nao khac dang ngoai duong. Truoc day mot xe chi giu duoc
     * mot tuyen nen khong can kiem; nay xe co the co tuyen ngay 29 dang chay trong khi tuyen
     * ngay 30 bi huy — tra xe ve San sang luc do la sai. Xe bao tri thi giu nguyen bao tri.
     *
     * @param finishedRoute tuyen vua xong hoac vua huy, khong tinh vao phep kiem
     */
    @Transactional
    public void releaseIfIdle(Route finishedRoute) {
        Vehicle vehicle = finishedRoute.getVehicle();
        if (vehicle.getStatus() != VehicleStatus.IN_USE) {
            return;
        }
        if (routeRepository.existsByVehicleIdAndStatusInAndIdNot(
                vehicle.getId(), STATUSES_ON_THE_ROAD, finishedRoute.getId())) {
            return;
        }
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);
    }

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
         * Trang thai IN_USE do he thong dat khi tai xe nhan hang va go ra khi tuyen hoan thanh.
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
