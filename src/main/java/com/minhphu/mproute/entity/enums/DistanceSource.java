package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Nguon du lieu cua mot o trong ma tran khoang cach.
 *
 * OSRM loi thi dung Haversine nhan he so quy doi (CBR-11), khi do tuyen sinh ra
 * bi danh dau approximate_distance = true va bi loai khoi so lieu trung binh
 * trong bao cao de khong lam nhieu ket qua (BR08-03).
 */
@Getter
@RequiredArgsConstructor
public enum DistanceSource {

    OSRM("OSRM"),
    HAVERSINE("Haversine (xấp xỉ)");

    private final String displayName;
}
