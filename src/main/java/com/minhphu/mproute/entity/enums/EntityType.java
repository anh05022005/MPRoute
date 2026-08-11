package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Loai doi tuong duoc ghi trong bang status_history.
 *
 * Bang lich su tro toi hai bang khac nhau nen khong dung khoa ngoai duoc,
 * phai dung cap (entityType, entityId) de xac dinh ban ghi goc.
 */
@Getter
@RequiredArgsConstructor
public enum EntityType {

    ORDER("Đơn hàng"),
    ROUTE("Tuyến");

    private final String displayName;
}
