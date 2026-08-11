package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai tai khoan. LOCKED la khoa vinh vien do quan tri vien dat.
 * Khoa tam 15 phut do dang nhap sai 5 lan dung cot locked_until, khong dung enum nay.
 */
@Getter
@RequiredArgsConstructor
public enum UserStatus {

    ACTIVE("Hoạt động"),
    LOCKED("Khóa");

    private final String displayName;
}
