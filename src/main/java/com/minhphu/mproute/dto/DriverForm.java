package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.enums.RecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Du lieu form them/sua tai xe (CN-01-03).
 */
@Data
public class DriverForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập họ tên tài xế")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    /*
     * BR01-08: so dien thoai dung 10 chu so va bat dau bang 0.
     *
     * Bieu thuc co hai nhanh: "^$" cho chuoi rong di qua, "^0\d{9}$" la dinh dang that.
     * Vi sao cho chuoi rong di qua: o nay bo trong thi @NotBlank da bao roi, neu @Pattern
     * bao them nua thi man hinh hien chong hai dong loi cho cung mot o.
     * Con so dien thoai sai dinh dang (9 chu so, bat dau bang 9, co chu cai) van bi chan.
     */
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^$|^0\\d{9}$",
            message = "Số điện thoại phải đúng 10 chữ số và bắt đầu bằng số 0")
    private String phone;

    @Size(max = 30, message = "Số giấy phép lái xe tối đa 30 ký tự")
    private String licenseNumber;

    /*
     * Tai khoan dang nhap cua tai xe. Duoc phep de trong: co the tao ho so tai xe truoc,
     * cap tai khoan sau. Nhung chua co tai khoan thi tai xe khong dang nhap xem tuyen
     * cua minh duoc (BR05-01).
     */
    private Long userId;

    // Xe thuong lai. Duoc phep de trong, dieu phoi van doi xe khac luc chot tuyen
    private Long defaultVehicleId;

    private RecordStatus status = RecordStatus.ACTIVE;

    public static DriverForm from(Driver driver) {
        DriverForm form = new DriverForm();
        form.setId(driver.getId());
        form.setFullName(driver.getFullName());
        form.setPhone(driver.getPhone());
        form.setLicenseNumber(driver.getLicenseNumber());
        // Hai quan he duoi day duoc phep NULL nen phai kiem truoc khi lay id
        form.setUserId(driver.getUser() == null ? null : driver.getUser().getId());
        form.setDefaultVehicleId(
                driver.getDefaultVehicle() == null ? null : driver.getDefaultVehicle().getId());
        form.setStatus(driver.getStatus());
        return form;
    }
}
