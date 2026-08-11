package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Loai su co tren duong. Chon OTHER thi bat buoc nhap mo ta (BR06-01).
 */
@Getter
@RequiredArgsConstructor
public enum IncidentType {

    VEHICLE_BREAKDOWN("Xe hỏng"),
    TRAFFIC_JAM("Tắc đường"),
    ACCIDENT("Tai nạn"),
    OTHER("Khác");

    private final String displayName;
}
