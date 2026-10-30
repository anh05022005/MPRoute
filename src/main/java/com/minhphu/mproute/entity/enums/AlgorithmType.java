package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Thuat toan dung de sinh tuyen.
 *
 * OR_TOOLS la bo giai duy nhat cua he thong. MANUAL la tuyen do dieu phoi vien keo tha
 * chinh tay.
 *
 * NN va NN_2OPT KHONG con bo giai nao phia sau (docs/09 muc A7), khong hien o dau de
 * chon. Giu lai chi vi bang routes va optimization_results con nhung dong cu da luu gia
 * tri nay: xoa khoi enum thi Hibernate nem loi khi doc cac dong do, ma doi nhan cua chung
 * thanh OR_TOOLS la lam gia so lieu bao cao.
 */
@Getter
@RequiredArgsConstructor
public enum AlgorithmType {

    OR_TOOLS("OR-Tools (bộ giải chính)"),
    NN("Nearest Neighbor (đã bỏ)"),
    NN_2OPT("Nearest Neighbor + 2-opt (đã bỏ)"),
    MANUAL("Chỉnh tay");

    private final String displayName;
}
