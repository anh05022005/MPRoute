package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import com.minhphu.mproute.service.optimization.heuristic.TwoOptImprover;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hai test bat buoc cho 2-opt, cong mot test chung to no THUC SU cai thien duoc chu
 * khong phai chay xong roi de nguyen.
 *
 * JUnit thuan, ma tran chep tay tu hinh ve, khong sinh ngau nhien va khong mock gi.
 */
class TwoOptImproverTest {

    /*
     * ============================ HINH 1: HINH CHU NHAT ============================
     *
     *        C(0,3) ------------- B(4,3)
     *          |                    |
     *        3 |                    | 3
     *          |                    |
     *       KHO(0,0) ------------ A(4,0)
     *                   4
     *
     * Duong cheo KHO-B va A-C deu bang 5 (tam giac 3-4-5), nen moi o la so nguyen.
     * Don vi met.
     */
    private static final double[][] RECTANGLE = {
            /* tu KHO */ {    0, 4000, 5000, 3000},
            /* tu A   */ { 4000,    0, 3000, 5000},
            /* tu B   */ { 5000, 3000,    0, 4000},
            /* tu C   */ { 3000, 5000, 4000,    0}
    };

    /*
     * ============================ HINH 2: NAM DIEM TREN MOT DUONG THANG ============
     *
     *      KHO(0)   P1(1)   P2(2)   P3(3)   P4(4)   P5(5)      don vi km
     *
     * Khoang cach = hieu toa do, nen ma tran duoi day tinh nham mat.
     *
     * DAP SO TOI UU CHUNG MINH DUOC BANG LOI: xe phai ra toi P5 o km 5 roi quay ve kho,
     * nen bat ky thu tu nao cung phai di it nhat 5 km di va 5 km ve = 10 km. Thu tu
     * sap tang dan dat dung 10 km, nen do la toi uu tuyet doi chu khong phai "toi uu
     * theo cai code cua em tinh ra".
     */
    private static final double[][] LINE = buildLineMatrix(new double[]{0, 1, 2, 3, 4, 5});

    private static double[][] buildLineMatrix(double[] positionsKm) {
        int size = positionsKm.length;
        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = Math.abs(positionsKm[i] - positionsKm[j]) * 1000;
            }
        }
        return matrix;
    }

    private OptimizationPoint point(int matrixIndex, String name) {
        OptimizationPoint point = OptimizationPoint.forOrder(
                (long) matrixIndex, name, 0, 0, 100, 1);
        point.setMatrixIndex(matrixIndex);
        return point;
    }

    private OptimizationRoute routeOf(double[][] matrix, int... matrixIndexes) {
        OptimizationRoute route = new OptimizationRoute(
                new OptimizationVehicle(1L, "29H-123.45", 100_000, 1_000));
        for (int matrixIndex : matrixIndexes) {
            route.addPoint(point(matrixIndex, "P" + matrixIndex));
        }
        RouteDistanceCalculator.updateTotalDistance(route, matrix);
        return route;
    }

    /**
     * TEST BAT BUOC: sau 2-opt tong quang duong luon <= truoc 2-opt.
     *
     * Day la tinh chat DINH NGHIA cua 2-opt: no chi nhan mot phep dao khi do lech am,
     * nen ve mat toan hoc khong bao gio lam tuyen dai ra. Test do lai tinh chat do tren
     * du lieu that, va no chinh la cho phat hien loi neu ma tran bi bat doi xung (luc
     * do cong thuc bon canh sai, 2-opt tuong cai thien ma thuc te lam dai them).
     */
    @Test
    void twoOptNeverIncreasesTotalDistance() {

        TwoOptImprover improver = new TwoOptImprover();

        /*
         * TRUONG HOP 1 — HINH CHU NHAT, tuyen bi "that nut".
         *
         * Truoc : KHO -> B -> A -> C -> KHO = 5000 + 3000 + 5000 + 3000 = 16000 m
         * Sau   : KHO -> A -> B -> C -> KHO = 4000 + 3000 + 4000 + 3000 = 14000 m
         *
         * Chay tay phep dao dau tien de doi chieu voi code:
         *     i = 0, j = 1  ->  a = KHO, b = B, c = A, d = C
         *     delta = D[KHO][A] + D[B][C] - D[KHO][B] - D[A][C]
         *           = 4000     + 4000    - 5000      - 5000     = -2000  (am, nhan dao)
         * Dao doan [0..1] cua [B, A, C] duoc [A, B, C]. Tuyen ngan di dung 2000 m.
         *
         * 14000 m la CHU VI hinh chu nhat, khong the ngan hon: tuyen nao di het bon dinh
         * cung phai di it nhat mot vong quanh hinh.
         */
        OptimizationRoute crossedRoute = routeOf(RECTANGLE, 2, 1, 3);
        double before = crossedRoute.getTotalDistance();
        assertEquals(16000, before, 0.001, "Tuyen KHO->B->A->C->KHO phai la 16000 m");

        improver.improve(crossedRoute, RECTANGLE);
        double after = crossedRoute.getTotalDistance();

        assertTrue(after <= before, "2-opt lam tuyen DAI THEM: " + before + " -> " + after);
        assertEquals(14000, after, 0.001,
                "Go xong nut phai ra chu vi hinh chu nhat 14000 m");

        /*
         * TRUONG HOP 2 — NAM DIEM TREN DUONG THANG, thu tu xao tron nang.
         *
         * Truoc: KHO -> P3 -> P1 -> P5 -> P2 -> P4 -> KHO
         *      = |0-3| + |3-1| + |1-5| + |5-2| + |2-4| + |4-0|
         *      =   3   +   2   +   4   +   3   +   2   +   4   = 18 km
         *
         * Sau : phai ve dung 10 km — chan duoi da chung minh o phan khai bao ma tran.
         */
        OptimizationRoute scrambledRoute = routeOf(LINE, 3, 1, 5, 2, 4);
        double scrambledBefore = scrambledRoute.getTotalDistance();
        assertEquals(18000, scrambledBefore, 0.001,
                "Tuyen xao tron phai la 18000 m truoc khi cai thien");

        improver.improve(scrambledRoute, LINE);
        double scrambledAfter = scrambledRoute.getTotalDistance();

        assertTrue(scrambledAfter <= scrambledBefore,
                "2-opt lam tuyen DAI THEM: " + scrambledBefore + " -> " + scrambledAfter);
        assertTrue(scrambledAfter >= 10000,
                "Khong the ngan hon 10000 m — xe van phai ra toi P5 roi quay ve");

        /*
         * TRUONG HOP 3 — tuyen DA TOI UU SAN. 2-opt phai de nguyen, khong duoc dao lung
         * tung roi lam dai ra. Day la cho bat loi nguong so sanh: neu viet "delta < 0"
         * thay vi "delta < -0.001" thi sai so double co the lam ham nhan mot phep dao
         * vo nghia o day.
         */
        OptimizationRoute optimalRoute = routeOf(LINE, 1, 2, 3, 4, 5);
        assertEquals(10000, optimalRoute.getTotalDistance(), 0.001);

        improver.improve(optimalRoute, LINE);
        assertEquals(10000, optimalRoute.getTotalDistance(), 0.001,
                "Tuyen da toi uu thi 2-opt phai de nguyen");
    }

    /**
     * TEST BAT BUOC: 2-opt khong lam thay doi TAP diem cua tuyen.
     *
     * DAY LA CAU TRA LOI CHO CAU HOI "lam sao em chac dao doan khong lam xe qua tai".
     * 2-opt chi doi cho cac phan tu san co trong danh sach: khong them diem, khong bot
     * diem, khong chuyen diem sang tuyen khac. Tap diem giu nguyen thi tong kg va tong
     * m3 giu nguyen, nen rang buoc suc chua da thoa man o giai doan 1 tiep tuc thoa man
     * sau giai doan 2 — khong can kiem lai lan nao.
     *
     * Chinh vi tinh chat nay ma hai giai doan tach roi nhau duoc.
     */
    @Test
    void twoOptPreservesPointSet() {

        OptimizationRoute route = routeOf(LINE, 3, 1, 5, 2, 4);

        int pointCountBefore = route.getPoints().size();
        double weightBefore = route.getTotalWeight();
        double volumeBefore = route.getTotalVolume();
        List<Long> orderIdsBefore = orderIdsOf(route);

        new TwoOptImprover().improve(route, LINE);

        assertEquals(pointCountBefore, route.getPoints().size(),
                "2-opt lam mat hoac lam thua diem");
        assertEquals(weightBefore, route.getTotalWeight(), 0.001,
                "Tong khoi luong doi sau 2-opt — khong the xay ra neu chi doi cho");
        assertEquals(volumeBefore, route.getTotalVolume(), 0.001,
                "Tong the tich doi sau 2-opt — khong the xay ra neu chi doi cho");

        // Cung mot TAP diem, du thu tu da khac
        List<Long> orderIdsAfter = orderIdsOf(route);
        assertEquals(orderIdsBefore.size(), orderIdsAfter.size());
        assertTrue(orderIdsAfter.containsAll(orderIdsBefore),
                "Sau 2-opt xuat hien diem la hoac mat diem cu");

        /*
         * Thu tu thi PHAI doi — khong doi nghia la 2-opt khong chay lan nao.
         *
         * KHONG khang dinh no ra dung [1,2,3,4,5]: bai nay co NHIEU loi giai cung dat
         * 10 km. Chay that ra [1,2,3,5,4]:
         *     0->1->2->3->5->4->0 = 1 + 1 + 1 + 2 + 1 + 4 = 10 km
         * dung bang [1,2,3,4,5]:
         *     0->1->2->3->4->5->0 = 1 + 1 + 1 + 1 + 1 + 5 = 10 km
         *
         * Ca hai deu toi uu. 2-opt dung ngay khi khong con phep dao nao co loi, no khong
         * co nghia vu tim ra loi giai NAO trong so cac loi giai bang diem. Vi vay test
         * chot bang TONG QUANG DUONG chu khong chot bang mot thu tu cu the — chot thu tu
         * la test se do khi doi thu tu duyet vong lap, du ket qua van toi uu.
         */
        assertTrue(!orderIdsBefore.equals(orderIdsAfter),
                "Thu tu khong doi chut nao — 2-opt khong chay");
        assertEquals(10000, route.getTotalDistance(), 0.001,
                "Sau 2-opt phai dat 10000 m, la chan duoi cua bai nay");
    }

    private List<Long> orderIdsOf(OptimizationRoute route) {
        List<Long> orderIds = new ArrayList<>();
        for (OptimizationPoint point : route.getPoints()) {
            orderIds.add(point.getOrderId());
        }
        return orderIds;
    }
}
