package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Muc do nghiem trong cua su co.
 *
 * CAN_CONTINUE: chi cong so phut cham tre vao ETA cac diem con lai, khong sinh phuong an.
 * CANNOT_CONTINUE: sinh 3 nhom phuong an A/B/C va chuyen xe sang MAINTENANCE (BR06-14).
 */
@Getter
@RequiredArgsConstructor
public enum IncidentSeverity {

    CAN_CONTINUE("Vẫn tiếp tục được"),
    CANNOT_CONTINUE("Không thể tiếp tục");

    private final String displayName;
}
