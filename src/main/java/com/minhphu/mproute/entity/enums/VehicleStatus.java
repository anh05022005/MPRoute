package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai xe.
 *
 * Chi xe AVAILABLE moi duoc dua vao phan tuyen (CBR-05).
 * IN_USE do he thong tu dat khi chot tuyen, quan tri vien khong chon tay duoc (BR01-04).
 * MAINTENANCE do quan tri vien dat, hoac he thong tu dat khi co su co CANNOT_CONTINUE.
 */
@Getter
@RequiredArgsConstructor
public enum VehicleStatus {

    AVAILABLE("Sẵn sàng"),
    IN_USE("Đang chạy"),
    MAINTENANCE("Bảo trì");

    private final String displayName;
}
