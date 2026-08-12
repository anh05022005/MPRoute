package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.DriverForm;
import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.repository.DriverRepository;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nghiep vu danh muc tai xe (CN-01-03).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public Page<Driver> search(String keyword, RecordStatus status, Pageable pageable) {
        // O tim kiem de trong gui len chuoi rong chu khong phai null.
        // Doi ve null de cau JPQL bo qua dieu kien do
        String tuKhoa = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return driverRepository.search(tuKhoa, status, pageable);
    }

    @Transactional(readOnly = true)
    public Driver getById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài xế"));
    }

    /**
     * Danh sach tai khoan do vao o chon: chi tai khoan vai tro DRIVER va CHUA gan
     * cho tai xe nao khac.
     *
     * currentDriverId la tai xe dang sua — tai khoan cua chinh no van phai co trong
     * danh sach, khong thi mo form sua ra la o chon bi rong.
     */
    @Transactional(readOnly = true)
    public List<User> selectableAccounts(Long currentDriverId) {
        List<Long> daGan = driverRepository.findLinkedUserIds(currentDriverId);
        return userRepository.findByRoleOrderByUsernameAsc(Role.DRIVER).stream()
                .filter(taiKhoan -> !daGan.contains(taiKhoan.getId()))
                .toList();
    }

    /**
     * Danh sach xe do vao o chon "Xe mac dinh".
     *
     * Liet ke ca xe dang bao tri va dang chay: day chi la xe THUONG LAI cua tai xe,
     * khong phai xe duoc phan cho tuyen hom nay. Viec loc xe san sang de chay tuyen
     * lam o man hinh phan tuyen (CBR-05), khong lam o day.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> selectableVehicles() {
        return vehicleRepository.findAll(Sort.by("licensePlate").ascending());
    }

    /**
     * Mot tai khoan chi gan duoc cho mot tai xe (cot user_id dat unique).
     *
     * excludeId dung khi dang SUA: tai xe tu giu tai khoan cua chinh no la binh thuong,
     * chi bao trung khi tai khoan do thuoc ve mot tai xe KHAC.
     */
    @Transactional(readOnly = true)
    public boolean isAccountTaken(Long userId, Long excludeId) {
        return driverRepository.findByUserId(userId)
                .filter(driver -> !driver.getId().equals(excludeId))
                .isPresent();
    }

    /** Tai khoan gan cho tai xe bat buoc phai co vai tro DRIVER. */
    @Transactional(readOnly = true)
    public boolean isDriverAccount(Long userId) {
        return userRepository.findById(userId)
                .map(taiKhoan -> taiKhoan.getRole() == Role.DRIVER)
                .orElse(false);
    }

    @Transactional
    public Driver save(DriverForm form) {
        // id null la them moi, co id la sua ban ghi dang co
        Driver driver = (form.getId() == null) ? new Driver() : getById(form.getId());

        driver.setFullName(form.getFullName().trim());
        driver.setPhone(form.getPhone().trim());
        driver.setLicenseNumber(chuoiHoacNull(form.getLicenseNumber()));
        driver.setStatus(form.getStatus());

        // Hai o chon duoi day de trong thi gan null, khong tra ve loi
        driver.setUser(form.getUserId() == null ? null
                : userRepository.findById(form.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản")));

        driver.setDefaultVehicle(form.getDefaultVehicleId() == null ? null
                : vehicleRepository.findById(form.getDefaultVehicleId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phương tiện")));

        Driver daLuu = driverRepository.save(driver);
        log.info("Luu tai xe {} - {}", daLuu.getFullName(), daLuu.getPhone());
        return daLuu;
    }

    /**
     * Doi giua ACTIVE va INACTIVE.
     *
     * CBR-17 / BR01-09: khong co ham xoa cung. Tai xe da chay tuyen cu ma bi xoa thi
     * bao cao cu mat thong tin ai chay tuyen nao. Tai xe INACTIVE la tai xe da nghi viec,
     * khong hien trong o chon luc chot tuyen nua nhung lich su van con.
     */
    @Transactional
    public RecordStatus toggleStatus(Long id) {
        Driver driver = getById(id);
        RecordStatus moi = (driver.getStatus() == RecordStatus.ACTIVE)
                ? RecordStatus.INACTIVE
                : RecordStatus.ACTIVE;
        driver.setStatus(moi);
        driverRepository.save(driver);
        log.info("Tai xe {} chuyen sang trang thai {}", driver.getFullName(), moi);
        return moi;
    }

    // O nhap khong bat buoc de trong gui len chuoi rong, luu vao DB nen la NULL cho gon
    private String chuoiHoacNull(String giaTri) {
        return (giaTri == null || giaTri.isBlank()) ? null : giaTri.trim();
    }
}
