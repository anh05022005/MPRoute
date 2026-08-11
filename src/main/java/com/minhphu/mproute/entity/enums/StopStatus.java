package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai mot diem dung trong tuyen.
 * PENDING -> DELIVERING -> DELIVERED / FAILED / POSTPONED
 */
@Getter
@RequiredArgsConstructor
public enum StopStatus {

    PENDING("Chờ giao"),
    DELIVERING("Đang giao"),
    DELIVERED("Đã giao"),
    FAILED("Giao thất bại"),
    POSTPONED("Hoãn");

    private final String displayName;
}
