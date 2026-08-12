package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.enums.RecordStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Du lieu form them/sua san pham (CN-01-01).
 *
 * Khong buoc thang vao entity Product: nguoi dung co the them truong an vao form
 * gui len, buoc vao entity la ho sua duoc ca nhung cot khong cho phep (mass assignment).
 * DTO chi chua dung nhung o thuc su co tren man hinh.
 */
@Data
public class ProductForm {

    // Null nghia la dang them moi, co gia tri nghia la dang sua
    private Long id;

    @NotBlank(message = "Vui lòng nhập mã sản phẩm")
    @Size(max = 20, message = "Mã sản phẩm tối đa 20 ký tự")
    private String code;

    @NotBlank(message = "Vui lòng nhập tên sản phẩm")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    @NotBlank(message = "Vui lòng nhập đơn vị tính")
    @Size(max = 20, message = "Đơn vị tính tối đa 20 ký tự")
    private String unit = "Thùng";

    /*
     * BR01-01: hai truong duoi day bat buoc va phai LON HON 0.
     * inclusive = false chinh la cho bien giua "lon hon 0" va "lon hon hoac bang 0" —
     * de sot cai nay thi nhap 0 van luu duoc, den tuan 6 thuat toan chia cho 0.
     */
    @NotNull(message = "Vui lòng nhập khối lượng mỗi thùng")
    @DecimalMin(value = "0.0", inclusive = false, message = "Khối lượng mỗi thùng phải lớn hơn 0")
    private Double weightPerBox;

    @NotNull(message = "Vui lòng nhập thể tích mỗi thùng")
    @DecimalMin(value = "0.0", inclusive = false, message = "Thể tích mỗi thùng phải lớn hơn 0")
    private Double volumePerBox;

    // Tien dung BigDecimal. Don gia duoc phep bang 0 (hang khuyen mai)
    @NotNull(message = "Vui lòng nhập đơn giá")
    @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    private RecordStatus status = RecordStatus.ACTIVE;

    /** Do entity da luu trong DB ra form de hien len man hinh sua. */
    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.setId(product.getId());
        form.setCode(product.getCode());
        form.setName(product.getName());
        form.setUnit(product.getUnit());
        form.setWeightPerBox(product.getWeightPerBox());
        form.setVolumePerBox(product.getVolumePerBox());
        form.setUnitPrice(product.getUnitPrice());
        form.setNote(product.getNote());
        form.setStatus(product.getStatus());
        return form;
    }
}
