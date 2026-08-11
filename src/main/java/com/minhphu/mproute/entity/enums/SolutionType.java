package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Phuong an xu ly su co, xep theo km phat sinh tang dan (tru C luon xep cuoi).
 *
 * A: dieu xe AVAILABLE tu kho ra sang hang.
 * B: chen cac diem con lai vao tuyen khac dang chay, dung Cheapest Insertion.
 * C: hoan sang hom sau, km phat sinh bang 0 nhung luon xep cuoi.
 */
@Getter
@RequiredArgsConstructor
public enum SolutionType {

    A_RESCUE_VEHICLE("Điều xe cứu hộ"),
    B_INSERT_OTHER_ROUTE("Chèn vào tuyến khác"),
    C_POSTPONE("Hoãn sang hôm sau"),
    MANUAL("Xử lý thủ công");

    private final String displayName;
}
