package com.minhphu.mproute.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mot ngay giao trong bao cao RPT-01 — thong ke don hang (CN-08-01).
 *
 * CSDL gop san theo NGAY roi tra ve; viec gom ngay thanh tuan hoac thang lam o tang
 * service. Ly do: JPQL khong co ham lay so tuan (WEEK() la ham rieng cua MySQL), ma
 * viet cau truy van rieng cho tung he quan tri thi mat luon tinh chuyen doi duoc.
 * Mot khoang toi da 365 ngay thi day cung chi la 365 dong, gom trong bo nho khong ton gi.
 */
public record DailyOrderCount(LocalDate day,
                              long orderCount,
                              Double totalWeight,
                              Double totalVolume,
                              BigDecimal totalAmount) {
}
