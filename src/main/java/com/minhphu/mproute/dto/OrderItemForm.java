package com.minhphu.mproute.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mot dong chi tiet tren form tao don (CN-02-02).
 *
 * CHI co hai o nguoi dung nhap: chon san pham va so thung. Khoi luong, the tich,
 * don gia, thanh tien deu do he thong tinh tu danh muc (BR02-04, BR02-13) — dua
 * chung vao form la mo duong cho nguoi dung sua tay bang cong cu ngoai trinh duyet.
 *
 * CO Y khong dat rang buoc @NotNull o day: man hinh luon co san vai dong trong
 * de go cho nhanh, dong trong bi bo qua khi luu. Rang buoc "it nhat mot dong"
 * (BR02-02) kiem o OrderController.
 */
@Data
@NoArgsConstructor
public class OrderItemForm {

    private Long productId;

    private Integer boxCount;

    /** Dung khi do don da luu ra form sua, va khi dung du lieu mau cho man hinh. */
    public OrderItemForm(Long productId, Integer boxCount) {
        this.productId = productId;
        this.boxCount = boxCount;
    }

    /** Dong khong chon san pham hoac so thung <= 0 coi nhu dong trong, khong luu. */
    public boolean isBlank() {
        return productId == null || boxCount == null || boxCount <= 0;
    }
}
