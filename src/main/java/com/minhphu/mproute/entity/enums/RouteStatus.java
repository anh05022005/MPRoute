package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai tuyen.
 *
 * DRAFT -> CONFIRMED -> IN_PROGRESS -> COMPLETED
 *                       IN_PROGRESS -> INCIDENT -> IN_PROGRESS
 *
 * Chu y phan biet voi VehicleStatus: tuyen dung IN_PROGRESS con xe dung IN_USE.
 * Xe bi khoa ngay khi tuyen duoc CHOT, con tuyen chi sang IN_PROGRESS
 * khi tai xe xac nhan nhan hang.
 */
@Getter
@RequiredArgsConstructor
public enum RouteStatus {

    DRAFT("Nháp"),
    CONFIRMED("Đã chốt"),
    IN_PROGRESS("Đang chạy"),
    INCIDENT("Có sự cố"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String displayName;
}
