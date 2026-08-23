package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RoutingResult;
import com.minhphu.mproute.service.optimization.heuristic.NearestNeighborSolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bon test bat buoc cua Nearest Neighbor (docs/10-quy-uoc-ky-thuat.md):
 *   nearestNeighborNeverExceedsWeightCapacity
 *   nearestNeighborNeverExceedsVolumeCapacity
 *   nearestNeighborAssignsEveryPointWhenCapacityAllows
 *   nearestNeighborReportsUnassignedPointsWhenCapacityRunsOut
 *
 * Dung chung mot hinh ve, ma tran viet tay, dap so cong tay ra duoc. JUnit thuan —
 * khong Spring context, khong MySQL, khong OSRM, chay het trong vai mili giay.
 */
class NearestNeighborSolverTest {

    /*
     * Ba khach dat tren hinh chu nhat 4 x 3 km quanh kho, don vi met:
     *
     *        C(0,3) ------------- B(4,3)
     *          |                    |
     *        3 |                    | 3
     *          |                    |
     *       KHO(0,0) ------------ A(4,0)
     *                   4
     *
     * Duong cheo KHO-B va A-C bang 5 (tam giac 3-4-5), nen moi o deu la so nguyen.
     *
     * Tu kho, diem gan nhat la C (3000), roi A (4000), xa nhat la B (5000).
     * Ba con so nay quyet dinh thu tu ma Nearest Neighbor chon — nho ky de doc test.
     */
    private static final double[][] MATRIX = {
            /* tu KHO */ {    0, 4000, 5000, 3000},
            /* tu A   */ { 4000,    0, 3000, 5000},
            /* tu B   */ { 5000, 3000,    0, 4000},
            /* tu C   */ { 3000, 5000, 4000,    0}
    };

    private static final int INDEX_A = 1;
    private static final int INDEX_B = 2;
    private static final int INDEX_C = 3;

    private OptimizationPoint point(int matrixIndex, String name, double weight, double volume) {
        OptimizationPoint p = OptimizationPoint.forOrder((long) matrixIndex, name, 0, 0, weight, volume);
        p.setMatrixIndex(matrixIndex);
        return p;
    }

    /** Ba diem A, B, C voi khoi luong va the tich chi dinh rieng cho tung test. */
    private List<OptimizationPoint> threePoints(double weightA, double weightB, double weightC,
                                                double volumeA, double volumeB, double volumeC) {
        List<OptimizationPoint> points = new ArrayList<>();
        points.add(point(INDEX_A, "A", weightA, volumeA));
        points.add(point(INDEX_B, "B", weightB, volumeB));
        points.add(point(INDEX_C, "C", weightC, volumeC));
        return points;
    }

    // ==================== Test 1 ====================

    @Test
    void nearestNeighborNeverExceedsWeightCapacity() {

        /*
         * Nhu cau: A 300 kg, B 400 kg, C 300 kg — the tich de nho de khong vuong rang buoc kia.
         * Xe:      xe1 700 kg, xe2 500 kg.
         *
         * CHAY TAY:
         *   Xe1 (700 kg, duyet truoc vi tai trong lon hon):
         *     - dung o kho, gan nhat la C (3000). 0 + 300 <= 700  -> nhan C. Dang cho 300.
         *     - dung o C, con A (5000) va B (4000). Gan nhat la B.
         *       300 + 400 = 700 <= 700 -> nhan B, VUA DUNG day tai. Dang cho 700.
         *     - dung o B, con A. 700 + 300 = 1000 > 700 -> khong nhan duoc -> dong tuyen.
         *     Tuyen 1 = [C, B], 700 kg.
         *   Xe2 (500 kg):
         *     - gan nhat tu kho la A (4000). 300 <= 500 -> nhan A.
         *     Tuyen 2 = [A], 300 kg.
         *
         * Truong hop "vua dung bang tai trong" la co y: cai dat nao viet nham dau >= thay vi >
         * se loai B ra va cho ket qua khac.
         */
        List<OptimizationPoint> points = threePoints(300, 400, 300, 0.1, 0.1, 0.1);
        List<OptimizationVehicle> vehicles = List.of(
                new OptimizationVehicle(1L, "XE-700", 700, 10),
                new OptimizationVehicle(2L, "XE-500", 500, 10));

        RoutingResult result = new NearestNeighborSolver().solve(points, vehicles, MATRIX);

        assertEquals(2, result.getRoutes().size(), "Phai sinh ra dung 2 tuyen");

        // Dieu kien cot loi: KHONG tuyen nao vuot tai trong cua xe minh
        for (OptimizationRoute route : result.getRoutes()) {
            assertTrue(route.getTotalWeight() <= route.getVehicle().getMaxWeight(),
                    "Tuyen cua xe " + route.getVehicle().getLicensePlate()
                            + " cho " + route.getTotalWeight() + " kg, vuot tai trong "
                            + route.getVehicle().getMaxWeight());
        }

        OptimizationRoute bigVehicleRoute = result.getRoutes().get(0);
        assertEquals("XE-700", bigVehicleRoute.getVehicle().getLicensePlate(),
                "Xe tai trong lon phai duoc duyet truoc");
        assertEquals(700, bigVehicleRoute.getTotalWeight(), 0.001, "Xe to phai nhan dung C roi B");
        assertEquals(List.of("C", "B"),
                bigVehicleRoute.getPoints().stream().map(OptimizationPoint::getName).toList(),
                "Thu tu phai la C truoc (gan kho nhat) roi den B");

        // Tuyen 1 = KHO->C->B->KHO = 3000 + 4000 + 5000 = 12000 m
        assertEquals(12000, bigVehicleRoute.getTotalDistance(), 0.001);

        // Tuyen 2 = KHO->A->KHO = 4000 + 4000 = 8000 m
        assertEquals(8000, result.getRoutes().get(1).getTotalDistance(), 0.001);

        assertEquals(20000, result.getOptimizedDistance(), 0.001,
                "Tong hai tuyen phai la 12000 + 8000");
    }

    // ==================== Test 2 ====================

    @Test
    void nearestNeighborNeverExceedsVolumeCapacity() {

        /*
         * Rang buoc THE TICH la cai chan o day, khoi luong thi thua thai:
         *   ba diem moi diem 100 kg / 1.0 m3, xe 1000 kg nhung thung chi 2.0 m3.
         *
         * Neu cai dat chi kiem khoi luong (loi kinh dien so 2) thi ca ba diem se duoc
         * nhoi len xe: 300 kg < 1000 kg, "hop le". Thuc te 3.0 m3 khong xep vua thung 2.0 m3.
         *
         * CHAY TAY: xe nhan C (1.0 m3), roi B (2.0 m3 = vua day thung), toi A thi
         * 2.0 + 1.0 = 3.0 > 2.0 -> dong tuyen. Con lai A chua gan.
         */
        List<OptimizationPoint> points = threePoints(100, 100, 100, 1.0, 1.0, 1.0);
        List<OptimizationVehicle> vehicles = List.of(
                new OptimizationVehicle(1L, "XE-THUNG-NHO", 1000, 2.0));

        RoutingResult result = new NearestNeighborSolver().solve(points, vehicles, MATRIX);

        assertEquals(1, result.getRoutes().size());
        OptimizationRoute route = result.getRoutes().get(0);

        for (OptimizationRoute r : result.getRoutes()) {
            assertTrue(r.getTotalVolume() <= r.getVehicle().getMaxVolume(),
                    "Tuyen cho " + r.getTotalVolume() + " m3, vuot the tich "
                            + r.getVehicle().getMaxVolume());
        }

        assertEquals(2, route.getPoints().size(),
                "The tich chi cho phep 2 diem du khoi luong con thua nhieu");
        assertEquals(2.0, route.getTotalVolume(), 0.001);
        assertEquals(300, route.getTotalWeight() + result.getUnassignedPoints().get(0).getWeight(), 0.001);

        assertEquals(1, result.getUnassignedPoints().size(),
                "Diem thu ba phai bi bo lai vi het cho, khong phai vi het tai trong");
    }

    // ==================== Test 3 ====================

    @Test
    void nearestNeighborAssignsEveryPointWhenCapacityAllows() {

        /*
         * Mot xe rong rai: 5000 kg / 50 m3 cho ba diem tong 1000 kg / 3 m3.
         * Khong duoc sot diem nao, va tuyen phai di dung thu tu gan nhat:
         * kho -> C (3000) -> B (4000) -> A (3000) -> kho (4000) = 14000 m.
         */
        List<OptimizationPoint> points = threePoints(300, 400, 300, 1.0, 1.0, 1.0);
        List<OptimizationVehicle> vehicles = List.of(
                new OptimizationVehicle(1L, "XE-TO", 5000, 50));

        RoutingResult result = new NearestNeighborSolver().solve(points, vehicles, MATRIX);

        assertEquals(1, result.getRoutes().size());
        assertEquals(3, result.assignedPointCount(), "Du suc chua thi khong duoc sot diem nao");
        assertTrue(result.getUnassignedPoints().isEmpty());
        assertFalse(result.hasUnassignedPoints());

        assertEquals(List.of("C", "B", "A"),
                result.getRoutes().get(0).getPoints().stream()
                        .map(OptimizationPoint::getName).toList(),
                "Nearest Neighbor phai di C -> B -> A: tu kho gan C nhat, tu C gan B nhat");

        assertEquals(14000, result.getOptimizedDistance(), 0.001);
    }

    // ==================== Test 4 ====================

    @Test
    void nearestNeighborReportsUnassignedPointsWhenCapacityRunsOut() {

        /*
         * Chi mot xe 500 kg cho ba diem tong 1000 kg — chac chan thieu.
         *
         * CHAY TAY: xe nhan C (300 kg, gan kho nhat). Tu C: B can 400 -> 700 > 500, loai;
         * A can 300 -> 600 > 500, loai. Dong tuyen. Con A va B chua gan.
         *
         * DIEU PHAI KIEM: danh sach diem sot duoc TRA VE, khong phai null va khong bi
         * im lang bo qua. Day la du lieu de man hinh hien MSG-08 / khoi canh bao CN-03-14.
         * Thuat toan im lang bo don la loi nghiep vu nghiem trong: dieu phoi vien tuong
         * da xep het, khach cho ca ngay khong thay hang.
         */
        List<OptimizationPoint> points = threePoints(300, 400, 300, 0.1, 0.1, 0.1);
        List<OptimizationVehicle> vehicles = List.of(
                new OptimizationVehicle(1L, "XE-NHO", 500, 10));

        RoutingResult result = new NearestNeighborSolver().solve(points, vehicles, MATRIX);

        assertNotNull(result.getUnassignedPoints(), "Phai la danh sach rong chu khong bao gio null");
        assertTrue(result.hasUnassignedPoints());
        assertEquals(2, result.getUnassignedPoints().size(), "A va B phai nam trong danh sach sot");

        assertEquals(1, result.assignedPointCount());
        assertEquals("C", result.getRoutes().get(0).getPoints().get(0).getName());

        // Tong so diem khong duoc mat mat: da gan + con sot = so diem dua vao
        assertEquals(3, result.assignedPointCount() + result.getUnassignedPoints().size());

        // Va tuyen sinh ra van phai ton trong rang buoc
        assertTrue(result.getRoutes().get(0).getTotalWeight() <= 500);
    }
}
