package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Du lieu form tao / sua don hang (CN-02-02).
 *
 * KHONG co ba o totalWeight, totalVolume, totalAmount: he thong tu tinh tu cac dong
 * chi tiet (BR02-04). Ba con so hien tren man hinh chi la JavaScript tinh truoc cho
 * nguoi nhap nhin, khi luu may chu tinh lai tu dau — nguoi dung gui gia tri khac len
 * cung khong an thua.
 *
 * Cung khong co o status: don moi luon la PENDING_APPROVAL (BR02-09), doi trang thai
 * phai di qua nut "Duyet don" hoac "Huy don" rieng.
 */
@Data
public class OrderForm {

    // Null nghia la dang them moi, co gia tri nghia la dang sua
    private Long id;

    // Chi de hien lai tren man hinh sua, khong dung khi luu
    private String code;

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Long customerId;

    /*
     * BR02-06: ngay giao mac dinh la ngay mai, khong nho hon hom nay.
     * @FutureOrPresent cua Bean Validation cung lam duoc, nhung thong bao mac dinh
     * cua no la tieng Anh va so sanh theo mui gio may ao Java — viet tay ro rang hon.
     * Cho kiem that nam o OrderController.
     */
    @NotNull(message = "Vui lòng chọn ngày giao")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deliveryDate = LocalDate.now().plusDays(1);

    /*
     * Chep tu khach hang khi chon khach (BR02-07), sua duoc cho rieng don nay.
     * Sua o day KHONG doi dia chi trong danh muc khach hang.
     */
    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String deliveryAddress;

    /*
     * BR02-08: ghi de dia chi thi bat buoc xac dinh lai toa do. Chan o OrderController
     * bang cach doi chieu voi dia chi va toa do trong danh muc khach hang.
     */
    @NotNull(message = "Vui lòng nhập vĩ độ")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    private Double latitude;

    @NotNull(message = "Vui lòng nhập kinh độ")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    private Double longitude;

    // Dien san theo khach hang, sua duoc cho rieng don (BR02-15).
    // Doi o nay la so tien phai thu doi theo (BR02-16), tinh lai o tang service
    @NotNull(message = "Vui lòng chọn hình thức thanh toán")
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    /*
     * Cac dong chi tiet. Khoi tao san ba dong trong de man hinh moi mo da co cho go ngay.
     * Spring tu noi dai danh sach nay khi trinh duyet gui len items[3], items[4]...
     * (tinh nang auto-grow cua WebDataBinder), nen them dong bang JavaScript van binh thuong.
     */
    private List<OrderItemForm> items = new ArrayList<>(List.of(
            new OrderItemForm(), new OrderItemForm(), new OrderItemForm()));

    /** Do don da luu trong CSDL ra form de hien len man hinh sua (CN-02-08). */
    public static OrderForm from(Order order) {
        OrderForm form = new OrderForm();
        form.setId(order.getId());
        form.setCode(order.getCode());
        form.setCustomerId(order.getCustomer().getId());
        form.setDeliveryDate(order.getDeliveryDate());
        form.setDeliveryAddress(order.getDeliveryAddress());
        form.setLatitude(order.getLatitude());
        form.setLongitude(order.getLongitude());
        form.setPaymentMethod(order.getPaymentMethod());
        form.setNote(order.getNote());

        List<OrderItemForm> dong = new ArrayList<>();
        for (var item : order.getItems()) {
            dong.add(new OrderItemForm(item.getProduct().getId(), item.getBoxCount()));
        }
        // Them mot dong trong o cuoi de them san pham moi ma khong phai bam "Them dong"
        dong.add(new OrderItemForm());
        form.setItems(dong);
        return form;
    }
}
