package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.PlannedStockMovement;
import com.minhphu.mproute.dto.StockSettlementPlan;
import com.minhphu.mproute.dto.StockShortage;
import com.minhphu.mproute.entity.enums.StockMovementType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tinh xem mot thay doi cam ket hang can ghi nhung dong so kho nao.
 *
 * <p>LOP NAY KHONG DOC CSDL, khong goi repository, khong goi OSRM. No nhan vao ba cai
 * Map so nguyen va tra ve mot danh sach y dinh. Nho vay toan bo phan de sai — phep tru,
 * phep bu, phat hien thieu hang — test duoc bang JUnit thuan voi du lieu cung tinh tay
 * duoc, khong can MySQL, khong can Spring context. Phan con lai cua StockService chi la
 * doc ghi, nghiem thu bang cach bam chuot.
 *
 * <p>Day cung la cach do an doi xu voi service/optimization: logic thuan tach khoi I/O.
 *
 * <h2>Vi sao chi can mot ham cho ba nghiep vu</h2>
 *
 * Ba viec khac nhau tren man hinh thuc ra la cung mot phep tinh, chi khac o hai Map dau vao:
 *
 * <pre>
 *   Duyet don       previousCommitment = rong          requestedCommitment = cac dong cua don
 *   Sua don da duyet previousCommitment = so luong cu   requestedCommitment = so luong moi
 *   Huy / that bai   previousCommitment = cac dong      requestedCommitment = rong
 * </pre>
 *
 * Mot ham, mot bo test, ba nghiep vu. Neu viet ba nhanh if rieng thi ba nhanh do se
 * lech nhau dan sau vai lan sua.
 */
@Component
public class StockSettlementPlanner {

    /**
     * @param availableStock       so thung CON RANH hien tai, theo productId. Thieu khoa coi nhu 0
     * @param previousCommitment   so thung don nay DANG cam ket truoc thay doi
     * @param requestedCommitment  so thung don nay MUON cam ket sau thay doi
     */
    public StockSettlementPlan plan(Map<Long, Integer> availableStock,
                                    Map<Long, Integer> previousCommitment,
                                    Map<Long, Integer> requestedCommitment) {

        // LinkedHashSet de thu tu dong so kho on dinh giua cac lan chay — test so sanh
        // theo thu tu, va so kho doc ra cung de hon khi thu tu khong nhay
        Set<Long> touchedProducts = new LinkedHashSet<>(previousCommitment.keySet());
        touchedProducts.addAll(requestedCommitment.keySet());

        List<PlannedStockMovement> movements = new ArrayList<>();
        List<StockShortage> shortages = new ArrayList<>();

        for (Long productId : touchedProducts) {
            int previous = previousCommitment.getOrDefault(productId, 0);
            int requested = requestedCommitment.getOrDefault(productId, 0);
            int delta = requested - previous;

            if (delta == 0) {
                // Sua dia chi hoac ghi chu ma khong doi so thung thi khong ghi dong nao.
                // Ghi mot cap OUT/RETURN bang nhau o day la lam so kho day dong rac
                continue;
            }

            if (delta > 0) {
                // Can lay them hang ra khoi phan con ranh
                int available = availableStock.getOrDefault(productId, 0);
                if (available < delta) {
                    shortages.add(new StockShortage(productId, delta, available));
                    continue;
                }
                movements.add(new PlannedStockMovement(productId, StockMovementType.OUT, delta));
            } else {
                // Tra lai hang cho kho. box_count luon duong, dau nam trong movementType
                movements.add(new PlannedStockMovement(productId, StockMovementType.RETURN, -delta));
            }
        }

        // Thieu bat ky mot san pham nao thi huy ca ke hoach: khong tru mot phan roi moi dung
        if (!shortages.isEmpty()) {
            return new StockSettlementPlan(List.of(), List.copyOf(shortages));
        }

        return new StockSettlementPlan(List.copyOf(movements), List.of());
    }
}
