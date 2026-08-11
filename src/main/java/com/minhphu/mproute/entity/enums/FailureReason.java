package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ly do giao that bai. Tai xe bat buoc phai chon mot ly do,
 * chon OTHER thi bat buoc nhap them mo ta (BR05-06).
 */
@Getter
@RequiredArgsConstructor
public enum FailureReason {

    CUSTOMER_ABSENT("Khách không có mặt"),
    CUSTOMER_REFUSED("Khách từ chối nhận"),
    WRONG_ADDRESS("Sai địa chỉ"),
    DAMAGED_GOODS("Hàng hư hỏng"),
    OTHER("Khác");

    private final String displayName;
}
