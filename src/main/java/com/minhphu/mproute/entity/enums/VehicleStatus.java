package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai xe.
 *
 * Trang thai nay noi xe DANG O DAU luc nay, khong noi xe ranh ngay nao. Xe ranh ngay D hay
 * khong thi tra theo bang routes cua ngay D (CBR-05, VehicleService.findSchedulableOn).
 *
 * IN_USE do he thong tu dat khi tai xe nhan hang (xe roi kho), quan tri vien khong chon tay
 * duoc (BR01-04). Truoc day dat ngay luc chot tuyen — xem docs/09 muc B6 vi sao da bo.
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
