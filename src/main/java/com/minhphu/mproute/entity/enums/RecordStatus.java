package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai chung cho cac ban ghi danh muc: products, customers, drivers.
 * Ban ghi da phat sinh du lieu nghiep vu khong xoa cung ma chuyen INACTIVE (CBR-17).
 */
@Getter
@RequiredArgsConstructor
public enum RecordStatus {

    ACTIVE("Đang sử dụng"),
    INACTIVE("Ngừng sử dụng");

    private final String displayName;
}
