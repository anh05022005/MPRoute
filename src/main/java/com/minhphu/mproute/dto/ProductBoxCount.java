package com.minhphu.mproute.dto;

/**
 * So thung da giao cua mot san pham trong ky (CN-08-01).
 *
 * Day la nua thu hai cua RPT-01: khong chi "bao nhieu don" ma con "bao nhieu thung moi
 * mat hang". Voi Minh Phu thi con so thu hai moi la cai dung de len ke hoach san xuat.
 */
public record ProductBoxCount(String productCode,
                              String productName,
                              Long boxCount,
                              Double totalWeight,
                              Double totalVolume,
                              Long orderCount) {
}
