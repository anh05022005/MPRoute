package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Hinh thuc thanh toan, gan co dinh voi tung khach hang nhung ghi de duoc theo tung don.
 * So tien phai thu tinh theo BR02-14: CASH thi bang tong tien hang, con lai bang 0.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    CREDIT("Công nợ"),
    CASH("Tiền mặt"),
    PREPAID("Chuyển khoản trước");

    private final String displayName;
}
