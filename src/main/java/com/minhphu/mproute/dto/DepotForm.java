package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Depot;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Du lieu form them/sua kho (CN-01-06).
 */
@Data
public class DepotForm {

    // Null nghia la dang them moi, co gia tri nghia la dang sua
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên kho")
    @Size(max = 100, message = "Tên kho tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Vui lòng nhập địa chỉ kho")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    /*
     * Toa do kho la diem c0 cua bai toan CVRP — moi tuyen deu xuat phat va ket thuc
     * o day (CBR-01). Sai toa do kho thi SAI TOAN BO ma tran khoang cach, khong phai
     * sai mot diem. Vi vay kho bat buoc co toa do y het khach hang.
     *
     * Chan theo mien gia tri hop le. Nham lan hay gap nhat la nhap doi cho vi do
     * va kinh do — kinh do Ha Noi khoang 105.8, vuot 90 nen dat vao o vi do bi chan ngay.
     */
    @NotNull(message = "Vui lòng nhập vĩ độ")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    private Double latitude;

    @NotNull(message = "Vui lòng nhập kinh độ")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    private Double longitude;

    // BR01-10: chi mot kho duoc active. Rang buoc that nam o DepotService
    private Boolean active = Boolean.TRUE;

    public static DepotForm from(Depot depot) {
        DepotForm form = new DepotForm();
        form.setId(depot.getId());
        form.setName(depot.getName());
        form.setAddress(depot.getAddress());
        form.setLatitude(depot.getLatitude());
        form.setLongitude(depot.getLongitude());
        form.setActive(depot.getActive());
        return form;
    }
}
