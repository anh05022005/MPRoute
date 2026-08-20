package com.minhphu.mproute.dto;

import java.math.BigDecimal;

/**
 * Ba con so tong cua mot don hang, do he thong tinh tu cac dong chi tiet (BR02-04).
 *
 * Gom lai mot record vi ba con so nay luon di voi nhau: tinh mot lan roi vua dung
 * de kiem suc chua (BR02-05), vua dung de luu xuong don.
 *
 * Khoi luong va the tich dung double (dau vao thuat toan), tien dung BigDecimal.
 */
public record OrderTotals(double khoiLuong, double theTich, BigDecimal tienHang) {
}
