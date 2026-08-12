package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Du lieu form them/sua phuong tien (CN-01-02).
 */
@Data
public class VehicleForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập biển số xe")
    @Size(max = 20, message = "Biển số tối đa 20 ký tự")
    private String licensePlate;

    @NotBlank(message = "Vui lòng nhập loại xe")
    @Size(max = 50, message = "Loại xe tối đa 50 ký tự")
    private String vehicleType;

    /*
     * BR01-03: hai truong duoi day bat buoc va phai LON HON 0.
     * Day chinh la HAI RANG BUOC ma thuat toan kiem DONG THOI khi xep don len xe
     * (CBR-03 khoi luong, CBR-04 the tich). Xe co tai trong bang 0 thi khong
     * xep duoc don nao, ma he thong lai khong bao gi — nen chan ngay tu khau nhap.
     */
    @NotNull(message = "Vui lòng nhập tải trọng tối đa")
    @DecimalMin(value = "0.0", inclusive = false, message = "Tải trọng tối đa phải lớn hơn 0")
    private Double maxWeight;

    @NotNull(message = "Vui lòng nhập thể tích thùng xe")
    @DecimalMin(value = "0.0", inclusive = false, message = "Thể tích thùng xe phải lớn hơn 0")
    private Double maxVolume;

    /*
     * Mac dinh AVAILABLE. Neu form khong gui o nay len (truong hop dang sua xe
     * DANG CHAY, o do hien dang chu chu khong phai o chon) thi truong nay giu
     * nguyen gia tri mac dinh, va tang service se bo qua no.
     */
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    public static VehicleForm from(Vehicle vehicle) {
        VehicleForm form = new VehicleForm();
        form.setId(vehicle.getId());
        form.setLicensePlate(vehicle.getLicensePlate());
        form.setVehicleType(vehicle.getVehicleType());
        form.setMaxWeight(vehicle.getMaxWeight());
        form.setMaxVolume(vehicle.getMaxVolume());
        form.setStatus(vehicle.getStatus());
        form.setNote(vehicle.getNote());
        return form;
    }
}
