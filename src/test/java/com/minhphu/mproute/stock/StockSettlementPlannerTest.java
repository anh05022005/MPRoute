package com.minhphu.mproute.stock;

import com.minhphu.mproute.dto.PlannedStockMovement;
import com.minhphu.mproute.dto.StockSettlementPlan;
import com.minhphu.mproute.dto.StockShortage;
import com.minhphu.mproute.entity.enums.StockMovementType;
import com.minhphu.mproute.service.StockSettlementPlanner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cho phan tru/hoan ton kho.
 *
 * Khong thuoc 14 test bat buoc. Viet them theo dung dieu kien o .claude/rules/kiem-thu.md:
 * "mot quy tac nghiep vu de sai va hay dung" — phep bu tru ton kho chay o bon cho trong
 * OrderService va hai cho trong DeliveryService, sai mot cho la so ton lech am tham.
 *
 * JUnit thuan: khong Spring context, khong MySQL, khong mock. StockSettlementPlanner
 * khong doc CSDL nen dung that duoc.
 *
 * Du lieu cung, tinh tay duoc, lay theo dung san pham that trong docs/08:
 * SP001 Bong tay trang (id 1), SP004 Nuoc lau san (id 4).
 */
class StockSettlementPlannerTest {

    private static final Long COTTON_PAD = 1L;   // SP001 Bong tay trang
    private static final Long FLOOR_CLEANER = 4L; // SP004 Nuoc lau san

    private final StockSettlementPlanner planner = new StockSettlementPlanner();

    // ==================== 1. Duyet don ====================

    /**
     * Duyet don hai dong thi sinh dung hai dong OUT, so thung giu nguyen.
     *
     * Tinh tay: kho co 800 bong + 500 nuoc lau san; don lay 220 bong + 100 nuoc lau san.
     * Ca hai deu du, nen ra OUT 220 va OUT 100.
     */
    @Test
    void approvalPlansOneOutMovementPerLine() {
        StockSettlementPlan plan = planner.plan(
                Map.of(COTTON_PAD, 800, FLOOR_CLEANER, 500),
                Map.of(),
                Map.of(COTTON_PAD, 220, FLOOR_CLEANER, 100));

        assertTrue(plan.isFeasible());
        assertEquals(2, plan.movements().size());
        assertTrue(plan.movements().contains(
                new PlannedStockMovement(COTTON_PAD, StockMovementType.OUT, 220)));
        assertTrue(plan.movements().contains(
                new PlannedStockMovement(FLOOR_CLEANER, StockMovementType.OUT, 100)));
    }

    // ==================== 2. Thieu hang thi khong tru mot phan nao ====================

    /**
     * Day la test quan trong nhat cua lop nay.
     *
     * Don lay 220 bong (kho con 800 — du) va 600 nuoc lau san (kho con 500 — thieu 100).
     * Chi mot san pham thieu, nhung ke hoach phai RONG HOAN TOAN: khong duoc tru 220 bong
     * roi moi dung lai. Tru mot phan roi bao loi la loai loi de lai du lieu ban trong
     * so kho ma khong ai thay ngay — don khong duyet duoc, nhung 220 thung bong da
     * bien mat khoi phan con ranh va khong co gi hoan lai.
     */
    @Test
    void approvalIsRejectedWhenAnyProductIsShort() {
        StockSettlementPlan plan = planner.plan(
                Map.of(COTTON_PAD, 800, FLOOR_CLEANER, 500),
                Map.of(),
                Map.of(COTTON_PAD, 220, FLOOR_CLEANER, 600));

        assertFalse(plan.isFeasible());
        assertTrue(plan.movements().isEmpty(), "thieu mot san pham thi khong duoc tru san pham nao");

        assertEquals(1, plan.shortages().size());
        StockShortage shortage = plan.shortages().get(0);
        assertEquals(FLOOR_CLEANER, shortage.productId());
        assertEquals(600, shortage.requiredBoxes());
        assertEquals(500, shortage.availableBoxes());
        assertEquals(100, shortage.missingBoxes());
    }

    // ==================== 3. Sua don da duyet: chi bu phan chenh ====================

    /**
     * Don da duyet cam ket 220 thung, khach goi lai xin giam con 180.
     *
     * Tinh tay: chenh = 180 - 220 = -40, nen sinh dung MOT dong RETURN 40.
     * KHONG duoc sinh RETURN 220 roi OUT 180 — lam the thi so ton cuoi cung van dung
     * nhung so kho day cap dong bang nhau, doc khong ra chuyen gi da xay ra.
     */
    @Test
    void editingCommittedOrderPlansOnlyTheDifference() {
        StockSettlementPlan plan = planner.plan(
                Map.of(COTTON_PAD, 80),
                Map.of(COTTON_PAD, 220),
                Map.of(COTTON_PAD, 180));

        assertTrue(plan.isFeasible());
        assertEquals(
                List.of(new PlannedStockMovement(COTTON_PAD, StockMovementType.RETURN, 40)),
                plan.movements());
    }

    /**
     * Sua tang len thi chi can du hang cho PHAN TANG THEM, khong phai cho ca don.
     *
     * Tinh tay: dang cam ket 220, muon len 300, chenh = +80. Kho con ranh 50 thung
     * nen thieu 30. So "can" bao ra phai la 80 chu khong phai 300 — 220 thung kia
     * da tru tu luc duyet, doi hoi lai lan nua la dem hai lan.
     */
    @Test
    void editingUpwardOnlyNeedsStockForTheIncrease() {
        StockSettlementPlan plan = planner.plan(
                Map.of(COTTON_PAD, 50),
                Map.of(COTTON_PAD, 220),
                Map.of(COTTON_PAD, 300));

        assertFalse(plan.isFeasible());
        StockShortage shortage = plan.shortages().get(0);
        assertEquals(80, shortage.requiredBoxes(), "chi can hang cho phan tang them");
        assertEquals(50, shortage.availableBoxes());
        assertEquals(30, shortage.missingBoxes());
    }

    // ==================== 4. Huy / that bai / hoan: hoan du moi dong ====================

    /**
     * Huy don hoac bao giao that bai: hang quay ve kho, hoan du tung dong.
     *
     * requestedCommitment rong vi don khong con cam ket gi nua.
     */
    @Test
    void releasePlansReturnForEveryCommittedLine() {
        StockSettlementPlan plan = planner.plan(
                Map.of(COTTON_PAD, 0, FLOOR_CLEANER, 400),
                Map.of(COTTON_PAD, 220, FLOOR_CLEANER, 100),
                Map.of());

        assertTrue(plan.isFeasible());
        assertEquals(2, plan.movements().size());
        assertTrue(plan.movements().contains(
                new PlannedStockMovement(COTTON_PAD, StockMovementType.RETURN, 220)));
        assertTrue(plan.movements().contains(
                new PlannedStockMovement(FLOOR_CLEANER, StockMovementType.RETURN, 100)));
    }

    // ==================== 5. Khong doi so thung thi khong ghi gi ====================

    /**
     * Sua dia chi giao hoac ghi chu cua don da duyet, so thung khong doi.
     *
     * Phai ra ke hoach RONG. Neu sinh mot cap OUT 220 / RETURN 220 thi so ton cuoi
     * van dung, nhung moi lan sua ghi chu lai them hai dong vao so kho — vai tuan sau
     * mo so ra khong con doc duoc gi.
     */
    @Test
    void unchangedLineProducesNoMovement() {
        StockSettlementPlan plan = planner.plan(
                Map.of(COTTON_PAD, 80),
                Map.of(COTTON_PAD, 220),
                Map.of(COTTON_PAD, 220));

        assertTrue(plan.isFeasible());
        assertTrue(plan.movements().isEmpty());
    }

    // ==================== 6. Bat bien: cong ca so ra dung so ton ====================

    /**
     * Chay lai dung tam buoc cua vi du da tinh tay, kiem bat bien cua ca phan ton kho:
     * cong tat ca so thung CO DAU trong so kho phai ra dung so ton hien tai.
     *
     * <pre>
     *   buoc 1  san xuat nhap 300          IN  300   -> ton 300
     *   buoc 2  khach A dat 220, tao don   (khong ghi gi, don chua duyet)
     *   buoc 3  khach B dat 150, tao don   (khong ghi gi)
     *   buoc 4  duyet don A                OUT 220   -> ton  80
     *   buoc 5  duyet don B -> CHAN        (khong ghi gi, thieu 70 thung)
     *   buoc 6  sua don B con 80, duyet    OUT  80   -> ton   0
     *   buoc 7  giao A that bai, hang ve   RETURN 220 -> ton 220
     *   buoc 8  san xuat nhap 500          IN  500   -> ton 720
     *
     *   cong so: 300 - 220 - 80 + 220 + 500 = 720   KHOP
     * </pre>
     *
     * Hoi dong hoi "sao em biet so ton nay dung" thi day la cau tra loi: khong tin con
     * so dang luu, cong lai ca so va doi chieu.
     */
    @Test
    void ledgerSumEqualsStockAfterOfLastRow() {
        int stock = 0;
        int ledgerSum = 0;

        // buoc 1: nhap kho khong di qua planner, no la phat sinh truc tiep
        stock += 300;
        ledgerSum += 300;
        assertEquals(300, stock);

        // buoc 4: duyet don A 220 thung
        stock = applyAndAccumulate(stock, Map.of(), Map.of(COTTON_PAD, 220), 220, StockMovementType.OUT);
        ledgerSum -= 220;
        assertEquals(80, stock);

        // buoc 5: duyet don B 150 thung -> chan, khong ghi dong nao
        StockSettlementPlan rejected = planner.plan(
                Map.of(COTTON_PAD, stock), Map.of(), Map.of(COTTON_PAD, 150));
        assertFalse(rejected.isFeasible());
        assertTrue(rejected.movements().isEmpty());
        assertEquals(70, rejected.shortages().get(0).missingBoxes());
        assertEquals(80, stock, "don bi chan thi so ton khong duoc doi");

        // buoc 6: sua don B con 80 thung roi duyet
        stock = applyAndAccumulate(stock, Map.of(), Map.of(COTTON_PAD, 80), 80, StockMovementType.OUT);
        ledgerSum -= 80;
        assertEquals(0, stock);

        // buoc 7: don A giao that bai, 220 thung ve kho
        stock = applyAndAccumulate(stock, Map.of(COTTON_PAD, 220), Map.of(), 220, StockMovementType.RETURN);
        ledgerSum += 220;
        assertEquals(220, stock);

        // buoc 8: nhap kho them 500
        stock += 500;
        ledgerSum += 500;

        assertEquals(720, stock);
        assertEquals(720, ledgerSum, "cong ca so kho phai ra dung so ton dang luu");
    }

    /**
     * Chay planner mot lan, kiem no de nghi dung mot dong nhu mong doi, roi ap vao so ton.
     * Giong y dieu StockService lam that, chi khac la khong ghi xuong CSDL.
     */
    private int applyAndAccumulate(int currentStock,
                                   Map<Long, Integer> previousCommitment,
                                   Map<Long, Integer> requestedCommitment,
                                   int expectedBoxCount,
                                   StockMovementType expectedType) {

        StockSettlementPlan plan = planner.plan(
                Map.of(COTTON_PAD, currentStock), previousCommitment, requestedCommitment);

        assertTrue(plan.isFeasible());
        assertEquals(
                List.of(new PlannedStockMovement(COTTON_PAD, expectedType, expectedBoxCount)),
                plan.movements());

        return currentStock + expectedType.getSign() * expectedBoxCount;
    }
}
