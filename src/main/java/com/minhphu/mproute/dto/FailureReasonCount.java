package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.enums.FailureReason;

/**
 * So lan giao that bai theo tung ly do (CN-08-02).
 *
 * Doc tu route_stops chu khong tu orders: ly do that bai la thuoc tinh cua LAN GIAO,
 * khong phai cua don. Mot don bi hoan roi giao lai va lai that bai thi do la hai lan
 * that bai voi hai ly do khac nhau, ma cot trang thai cua don chi giu duoc cai cuoi.
 *
 * Chinh vi phai gom nhom theo ly do ma man hinh tai xe bat CHON ly do tu danh sach
 * thay vi go tu do (BR05-06).
 */
public record FailureReasonCount(FailureReason reason, long stopCount) {
}
