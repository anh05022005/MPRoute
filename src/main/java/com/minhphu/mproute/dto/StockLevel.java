package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.Product;

/**
 * Ba con so ton kho cua mot san pham, dung cho man hinh kho va form don hang.
 *
 * Chi MOT trong ba con so duoc luu trong CSDL:
 *
 * <pre>
 *   availableBoxes  = products.stock_boxes          <- luu
 *   committedBoxes  = dem tu order_items            <- tinh lai moi lan
 *   physicalBoxes   = availableBoxes + committedBoxes <- suy ra
 * </pre>
 *
 * "Con ranh" la so tra loi cau hoi cua khach qua dien thoai: con hang khong.
 * "Trong kho" la so nhan vien kho dem duoc khi di vao kho — lon hon, vi hang da duyet
 * cho don van nam day cho tai xe den lay.
 */
public record StockLevel(Product product, int availableBoxes, int committedBoxes) {

    public int physicalBoxes() {
        return availableBoxes + committedBoxes;
    }

    /** To mau canh bao tren man hinh khi hang gan het. */
    public boolean isLow() {
        return availableBoxes < 100;
    }
}
