package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai xu ly su co. Dieu phoi vien chon phuong an thi chuyen sang RESOLVED.
 */
@Getter
@RequiredArgsConstructor
public enum IncidentStatus {

    PENDING("Chờ xử lý"),
    RESOLVED("Đã xử lý");

    private final String displayName;
}
