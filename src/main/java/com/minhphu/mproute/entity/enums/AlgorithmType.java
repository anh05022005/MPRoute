package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Thuat toan dung de sinh tuyen.
 *
 * OR_TOOLS la bo giai chinh cua he thong, dat lam mac dinh.
 * NN va NN_2OPT la ban TU CAI DAT, giu lai lam moc so sanh o chuong thuc nghiem —
 * cho dieu phoi vien chon duoc de demo so sanh truoc hoi dong (BR03-11, CN-03-04).
 * MANUAL dung khi dieu phoi vien keo tha chinh tay.
 *
 * Thu tu khai bao chinh la thu tu hien ra o chon, nen dat OR_TOOLS len dau.
 */
@Getter
@RequiredArgsConstructor
public enum AlgorithmType {

    OR_TOOLS("OR-Tools (bộ giải chính)"),
    NN("Nearest Neighbor"),
    NN_2OPT("Nearest Neighbor + 2-opt"),
    MANUAL("Chỉnh tay");

    private final String displayName;
}
