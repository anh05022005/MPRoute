package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Thuat toan dung de sinh tuyen.
 *
 * Dieu phoi vien chon NN hoac NN_2OPT (mac dinh) tren man hinh phan tuyen (BR03-11).
 * Chuc nang cho chon nay la de demo so sanh truoc hoi dong, bat buoc phai co (CN-03-04).
 * MANUAL dung khi dieu phoi vien keo tha chinh tay.
 */
@Getter
@RequiredArgsConstructor
public enum AlgorithmType {

    NN("Nearest Neighbor"),
    NN_2OPT("Nearest Neighbor + 2-opt"),
    MANUAL("Chỉnh tay");

    private final String displayName;
}
