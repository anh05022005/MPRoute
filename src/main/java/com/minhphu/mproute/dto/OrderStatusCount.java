package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.enums.OrderStatus;

/** So don theo tung trang thai, nguon cua bieu do tron RPT-02 (CN-08-02). */
public record OrderStatusCount(OrderStatus status, long orderCount) {
}
