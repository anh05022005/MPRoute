package com.minhphu.mproute.dto;

/**
 * Mot san pham khong du hang de duyet don.
 *
 * Chi mang so, KHONG mang ten san pham: lop tinh toan khong doc CSDL nen khong biet ten.
 * Ghep thanh cau MSG-17 la viec cua StockService, cho co Product trong tay.
 */
public record StockShortage(Long productId, int requiredBoxes, int availableBoxes) {

    /** Thieu bao nhieu thung — con so dua thang vao MSG-17 cho nguoi dung doc. */
    public int missingBoxes() {
        return requiredBoxes - availableBoxes;
    }
}
