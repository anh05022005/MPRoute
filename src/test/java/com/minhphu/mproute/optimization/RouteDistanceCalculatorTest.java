package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test bat buoc so 1: tong quang duong PHAI gom chang cuoi ve kho (CBR-01).
 *
 * Day la loi kinh dien nhat cua do an VRP. Quen chang cuoi thi chuong trinh van chay,
 * tuyen van ve len ban do binh thuong, chi co moi con so trong bang so sanh chuong 4
 * la sai — va sai theo huong co loi cho minh, nen rat de tu lua.
 *
 * JUnit thuan, ma tran viet tay tu hinh ve, khong sinh ngau nhien.
 */
class RouteDistanceCalculatorTest {

    /*
     * Bon diem dat tren mot hinh chu nhat 4 x 3 km, don vi met:
     *
     *        C(0,3) ------------- B(4,3)
     *          |                    |
     *        3 |                    | 3
     *          |                    |
     *       KHO(0,0) ------------ A(4,0)
     *                   4
     *
     * Duong cheo KHO-B va A-C deu bang 5 (tam giac 3-4-5). Nho vay moi o trong ma tran
     * duoi day la so nguyen, cong tay ra dap so khong can may tinh.
     */
    private static final double[][] MATRIX = {
            /* tu KHO */ {    0, 4000, 5000, 3000},
            /* tu A   */ { 4000,    0, 3000, 5000},
            /* tu B   */ { 5000, 3000,    0, 4000},
            /* tu C   */ { 3000, 5000, 4000,    0}
    };

    private OptimizationPoint point(int matrixIndex, String name) {
        OptimizationPoint p = OptimizationPoint.forOrder(
                (long) matrixIndex, name, 0, 0, 100, 1);
        p.setMatrixIndex(matrixIndex);
        return p;
    }

    @Test
    void totalDistanceIncludesReturnLegToDepot() {

        /*
         * TUYEN BA DIEM: KHO -> A -> B -> C -> KHO
         *
         * Cong tay theo hinh tren:
         *     KHO -> A : 4000
         *     A   -> B : 3000
         *     B   -> C : 4000
         *     C -> KHO : 3000     <-- CHANG CUOI, cho hay bi quen
         *     ------------------
         *     tong     : 14000 m = 14 km
         *
         * Neu cai dat quen chang cuoi thi ket qua se la 11000 — test nay do lai dung
         * 3000 m chenh lech do.
         */
        OptimizationRoute route = new OptimizationRoute(
                new OptimizationVehicle(1L, "29H-123.45", 1500, 12));
        route.addPoint(point(1, "A"));
        route.addPoint(point(2, "B"));
        route.addPoint(point(3, "C"));

        double total = RouteDistanceCalculator.calculateTotalDistance(route, MATRIX);
        assertEquals(14000, total, 0.001,
                "Tuyen KHO->A->B->C->KHO phai la 4000+3000+4000+3000 = 14000 m");

        /*
         * TUYEN MOT DIEM: KHO -> A -> KHO = 4000 + 4000 = 8000 m.
         *
         * Truong hop nay bat loi kieu khac: cai dat nao chi cong "cac chang giua cac diem
         * giao" ma quen ca hai dau kho se tra ve 0 o day.
         */
        OptimizationRoute singleStopRoute = new OptimizationRoute(
                new OptimizationVehicle(2L, "29C-678.90", 500, 6));
        singleStopRoute.addPoint(point(1, "A"));

        assertEquals(8000, RouteDistanceCalculator.calculateTotalDistance(singleStopRoute, MATRIX), 0.001,
                "Tuyen mot diem van phai gom ca luot di lan luot ve");

        // Tuyen rong: xe khong roi kho nen khong di met nao
        OptimizationRoute emptyRoute = new OptimizationRoute(
                new OptimizationVehicle(3L, "29F-555.66", 1000, 8));
        assertEquals(0, RouteDistanceCalculator.calculateTotalDistance(emptyRoute, MATRIX), 0.001);

        /*
         * DAO NGUOC THU TU phai ra cung tong, vi ma tran nay doi xung.
         * KHO -> C -> B -> A -> KHO = 3000 + 4000 + 3000 + 4000 = 14000.
         * Day cung la tinh chat ma 2-opt dua vao o tuan 7.
         */
        OptimizationRoute reversedRoute = new OptimizationRoute(
                new OptimizationVehicle(1L, "29H-123.45", 1500, 12));
        reversedRoute.addPoint(point(3, "C"));
        reversedRoute.addPoint(point(2, "B"));
        reversedRoute.addPoint(point(1, "A"));

        assertEquals(14000, RouteDistanceCalculator.calculateTotalDistance(reversedRoute, MATRIX), 0.001);

        // Tong cua nhieu tuyen = cong don tung tuyen
        assertEquals(14000 + 8000,
                RouteDistanceCalculator.totalDistanceOfRoutes(List.of(route, singleStopRoute), MATRIX),
                0.001);
    }
}
