package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai thanh toan cua don hang.
 * He thong chi GHI NHAN trang thai, khong quan ly dong tien (muc 1).
 * Don CREDIT giao thanh cong se chuyen sang DEBT (BR05-14).
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    UNPAID("Chưa thanh toán"),
    PAID("Đã thanh toán"),
    DEBT("Ghi nợ");

    private final String displayName;
}
