package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.enums.PaymentMethod;
import com.minhphu.mproute.entity.enums.RecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Du lieu form them/sua khach hang (CN-01-04).
 *
 * Khong buoc thang vao entity Customer: nguoi dung co the them truong an vao form
 * gui len, buoc vao entity la ho sua duoc ca nhung cot khong cho phep.
 */
@Data
public class CustomerForm {

    // Null nghia la dang them moi, co gia tri nghia la dang sua
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên cửa hàng")
    @Size(max = 200, message = "Tên cửa hàng tối đa 200 ký tự")
    private String name;

    @Size(max = 100, message = "Tên người liên hệ tối đa 100 ký tự")
    private String contactPerson;

    /*
     * BR01-08: so dien thoai dung 10 chu so va bat dau bang 0.
     *
     * Bieu thuc co hai nhanh: "^$" cho chuoi rong di qua, "^0\d{9}$" la dinh dang that.
     * O nay bo trong thi @NotBlank da bao roi, cho @Pattern bao them nua thi man hinh
     * hien chong hai dong loi cho cung mot o.
     */
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^$|^0\\d{9}$",
            message = "Số điện thoại phải đúng 10 chữ số và bắt đầu bằng số 0")
    private String phone;

    // Dung de gui thong bao ETA sau khi chot tuyen (BR04-11, NTF-08)
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    /*
     * BR01-05 / CBR-07: toa do BAT BUOC. Thieu toa do thi diem khong dua vao ma tran
     * khoang cach duoc, don hang cua khach nay bi loai khoi danh sach cho phan tuyen.
     *
     * Chan theo mien gia tri hop le cua toa do dia ly. Nham lan hay gap nhat la
     * nhap doi cho vi do va kinh do — Ha Noi kinh do khoang 105.8, vuot 90 nen
     * dat vao o vi do la bi chan ngay tai day.
     */
    @NotNull(message = "Vui lòng nhập vĩ độ")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    private Double latitude;

    @NotNull(message = "Vui lòng nhập kinh độ")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    private Double longitude;

    // Gan co dinh voi khach, nhung ghi de duoc cho rieng tung don (BR02-15)
    @NotNull(message = "Vui lòng chọn hình thức thanh toán")
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    // BR01-07: van ban tu do — "Chi nhan buoi sang", "Xe lon khong vao duoc ngo"
    @Size(max = 500, message = "Ghi chú giao hàng tối đa 500 ký tự")
    private String deliveryNote;

    private RecordStatus status = RecordStatus.ACTIVE;

    /** Do entity da luu trong DB ra form de hien len man hinh sua. */
    public static CustomerForm from(Customer customer) {
        CustomerForm form = new CustomerForm();
        form.setId(customer.getId());
        form.setName(customer.getName());
        form.setContactPerson(customer.getContactPerson());
        form.setPhone(customer.getPhone());
        form.setEmail(customer.getEmail());
        form.setAddress(customer.getAddress());
        form.setLatitude(customer.getLatitude());
        form.setLongitude(customer.getLongitude());
        form.setPaymentMethod(customer.getPaymentMethod());
        form.setDeliveryNote(customer.getDeliveryNote());
        form.setStatus(customer.getStatus());
        return form;
    }
}
