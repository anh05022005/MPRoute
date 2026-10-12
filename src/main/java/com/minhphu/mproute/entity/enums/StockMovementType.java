package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Loai phat sinh ton kho.
 *
 * <p>Moi loai mang MOT dau co dinh. Nho vay cot box_count luon duong va cong thuc
 * tinh ton sau moi dong chi con mot cong thuc duy nhat:
 * {@code stockAfter = stockBefore + sign * boxCount}.
 *
 * <p>Day la ly do kiem ke phai tach thanh HAI loai tang/giam chu khong de mot loai
 * ADJUST mang so am: mot cot vua chua so duong vua chua so am thi moi cho doc no
 * deu phai nho kiem dau truoc, va chi can mot cho quen la sai ca so ton.
 *
 * <p>IN va hai loai ADJUST do nguoi dung bam nut. OUT va RETURN do he thong tu ghi
 * khi don hang chuyen trang thai — khong co man hinh nao go truc tiep hai loai do,
 * neu khong so kho se noi khac voi bang orders.
 */
@Getter
@RequiredArgsConstructor
public enum StockMovementType {

    IN("Nhập kho", 1),
    OUT("Xuất cho đơn", -1),
    RETURN("Hàng về kho", 1),
    ADJUST_UP("Kiểm kê tăng", 1),
    ADJUST_DOWN("Kiểm kê giảm", -1);

    private final String displayName;

    /** +1 lam tang ton kho, -1 lam giam. Khong bao gio bang 0. */
    private final int sign;

    /** Hai loai nay do he thong sinh ra theo trang thai don, nguoi dung khong hoan tac truc tiep. */
    public boolean isSystemGenerated() {
        return this == OUT || this == RETURN;
    }
}
