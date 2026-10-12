package com.minhphu.mproute.dto;

import java.util.List;

/**
 * Ket qua tinh toan cua {@link com.minhphu.mproute.service.StockSettlementPlanner}.
 *
 * Mot trong hai danh sach luon rong:
 *   - thieu hang  -> shortages co phan tu, movements RONG (khong tru mot phan nao ca)
 *   - du hang     -> shortages rong, movements la nhung dong can ghi
 *
 * Quy tac duoc an toan het hoac khong lam gi la co y. Tru kho duoc ba san pham roi
 * phat hien san pham thu tu thieu, da tru roi moi dung lai, la loai loi de lai du lieu
 * ban ma khong ai thay ngay. Test approvalIsRejectedWhenAnyProductIsShort kiem dieu nay.
 */
public record StockSettlementPlan(List<PlannedStockMovement> movements,
                                  List<StockShortage> shortages) {

    public boolean isFeasible() {
        return shortages.isEmpty();
    }
}
