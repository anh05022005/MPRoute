package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.enums.StockMovementType;

/**
 * Mot dong so kho ma {@link com.minhphu.mproute.service.StockSettlementPlanner} de nghi ghi.
 *
 * Chi la y dinh, chua ghi gi xuong CSDL. Nho tach hai buoc "tinh xem phai ghi gi" va
 * "ghi xuong" ma phan tinh toan test duoc bang JUnit thuan, khong can MySQL.
 */
public record PlannedStockMovement(Long productId, StockMovementType movementType, int boxCount) {
}
