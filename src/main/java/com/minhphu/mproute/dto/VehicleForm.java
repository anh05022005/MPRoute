package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Locale;
import java.util.regex.Matcher;

/**
 * Du lieu form them/sua phuong tien (CN-01-02).
 */
@Data
public class VehicleForm {

    // Bien so co 5 so thi viet "123.45", bien cu 4 so thi viet "1234"
    private static final java.util.regex.Pattern FIVE_DIGIT_PLATE =
            java.util.regex.Pattern.compile("^(\\d{2}[A-Z]{1,2})-?(\\d{3})\\.?(\\d{2})$");
    private static final java.util.regex.Pattern FOUR_DIGIT_PLATE =
            java.util.regex.Pattern.compile("^(\\d{2}[A-Z]{1,2})-?(\\d{4})$");

    private Long id;

    /*
     * BR01-03: bien so xe o to Viet Nam — 2 so ma tinh (11-99), 1 hoac 2 chu seri,
     * gach noi, roi 5 so dang "123.45" hoac 4 so dang "1234" (bien cu).
     * Vi du hop le: 29H-123.45, 29C-1234, 30LD-123.45.
     *
     * Bieu thuc co nhanh "^$" cho chuoi rong di qua, de o trong chi hien mot loi cua
     * @NotBlank chu khong chong them loi sai dinh dang.
     */
    @NotBlank(message = "Vui lòng nhập biển số xe")
    @Pattern(regexp = "^$|^(1[1-9]|[2-9]\\d)[A-Z]{1,2}-(\\d{3}\\.\\d{2}|\\d{4})$",
            message = "Biển số không đúng định dạng. Ví dụ: 29H-123.45 hoặc 29C-1234")
    private String licensePlate;

    @NotBlank(message = "Vui lòng nhập loại xe")
    @Size(max = 50, message = "Loại xe tối đa 50 ký tự")
    private String vehicleType;

    /*
     * BR01-03: hai truong duoi day la HAI RANG BUOC ma thuat toan kiem DONG THOI khi xep
     * don len xe (CBR-03 khoi luong, CBR-04 the tich), nen phai nam trong khoang co that.
     *
     * Khoang lay theo xe tai nhe giao hang noi thanh dang ban o Viet Nam:
     *   - nho nhat: xe van / tai nho kieu Suzuki Carry, tai 500-750 kg, thung 2-4 m3
     *   - pho bien: Kia K200, Hyundai H150, Isuzu QKR — 1-2,5 tan, thung 8-15 m3
     *   - lon nhat: tai 3,5-5 tan kieu Hino XZU, thung khoang 20 m3
     * Chan duoi 300 kg / 1 m3 de bat loi go thieu so (nhap "15" thay vi "1500"),
     * chan tren 5.000 kg / 25 m3 de bat loi go thua so hoac nham don vi (nhap gam,
     * nhap lit). Mot so sai o day lam sai toan bo ket qua phan tuyen ma khong ai bao.
     */
    @NotNull(message = "Vui lòng nhập tải trọng tối đa")
    @DecimalMin(value = "300", message = "Vui lòng nhập tải trọng tối đa từ 300 đến 5.000 kg")
    @DecimalMax(value = "5000", message = "Vui lòng nhập tải trọng tối đa từ 300 đến 5.000 kg")
    private Double maxWeight;

    @NotNull(message = "Vui lòng nhập thể tích thùng xe")
    @DecimalMin(value = "1", message = "Vui lòng nhập thể tích thùng xe từ 1 đến 25 m³")
    @DecimalMax(value = "25", message = "Vui lòng nhập thể tích thùng xe từ 1 đến 25 m³")
    private Double maxVolume;

    /*
     * Mac dinh AVAILABLE. Neu form khong gui o nay len (truong hop dang sua xe
     * DANG CHAY, o do hien dang chu chu khong phai o chon) thi truong nay giu
     * nguyen gia tri mac dinh, va tang service se bo qua no.
     */
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    /*
     * Chuan hoa truoc khi kiem: bo khoang trang, viet hoa, tu them gach noi va dau cham.
     * Go "29h 12345", "29H12345" hay "29H-123.45" deu thanh "29H-123.45" — mot xe chi co
     * mot cach viet, nho vay kiem trung bien so (isLicensePlateTaken) moi bat duoc.
     * Chuoi khong khop mau nao thi de nguyen cho @Pattern bao loi.
     */
    public void setLicensePlate(String licensePlate) {
        if (licensePlate == null) {
            this.licensePlate = null;
            return;
        }
        String compact = licensePlate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);

        Matcher fiveDigits = FIVE_DIGIT_PLATE.matcher(compact);
        if (fiveDigits.matches()) {
            this.licensePlate = fiveDigits.group(1) + "-" + fiveDigits.group(2) + "." + fiveDigits.group(3);
            return;
        }
        Matcher fourDigits = FOUR_DIGIT_PLATE.matcher(compact);
        this.licensePlate = fourDigits.matches()
                ? fourDigits.group(1) + "-" + fourDigits.group(2)
                : compact;
    }

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
