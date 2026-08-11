package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai don hang.
 *
 * PENDING_APPROVAL -> APPROVED -> ROUTED -> DELIVERING -> DELIVERED / FAILED / POSTPONED
 * PENDING_APPROVAL / APPROVED -> CANCELLED
 *
 * Don POSTPONED se duoc tao lai o ngay giao moi voi trang thai APPROVED (BR05-09).
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    PENDING_APPROVAL("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    ROUTED("Đã phân tuyến"),
    DELIVERING("Đang giao"),
    DELIVERED("Đã giao"),
    FAILED("Giao thất bại"),
    POSTPONED("Hoãn"),
    CANCELLED("Đã hủy");

    private final String displayName;
}
