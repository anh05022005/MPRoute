package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import com.minhphu.mproute.service.optimization.heuristic.CheapestInsertion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test bat buoc cho Cheapest Insertion — thuat toan dung khi xu ly su co (UC06):
 * mot xe hong giua duong, cac diem chua giao cua no phai chen vao tuyen cua xe khac.
 *
 * Ba viec duoc kiem, ca ba deu tinh tay ra dap so:
 *   1. Chon dung vi tri re nhat
 *   2. KHONG chen vao qua khu — cac diem xe da di qua roi thi khong dong vao duoc
 *   3. Xe het cho thi bao khong chen duoc, khong lang le chen bua
 */
class CheapestInsertionTest {

    /*
     * Bon diem tren mot hinh chu nhat 4 x 3 km, don vi met:
     *
     *        C(0,3) ------------- B(4,3)
     *          |                    |
     *        3 |                    | 3
     *          |                    |
     *       KHO(0,0) ------------ A(4,0)
     *                   4
     *
     * Hai duong cheo KHO-B va A-C deu bang 5 (tam giac 3-4-5).
     */
    private static final double[][] MATRIX = {
            /* tu KHO */ {    0, 4000, 5000, 3000},
            /* tu A   */ { 4000,    0, 3000, 5000},
            /* tu B   */ { 5000, 3000,    0, 4000},
            /* tu C   */ { 3000, 5000, 4000,    0}
    };

    private static final int A = 1;
    private static final int B = 2;
    private static final int C = 3;

    private OptimizationPoint point(int matrixIndex, String name, double weight, double volume) {
        OptimizationPoint point = OptimizationPoint.forOrder(
                (long) matrixIndex, name, 0, 0, weight, volume);
        point.setMatrixIndex(matrixIndex);
        return point;
    }

    private OptimizationRoute routeWith(double maxWeight, double maxVolume, int... matrixIndexes) {
        OptimizationRoute route = new OptimizationRoute(
                new OptimizationVehicle(1L, "29H-123.45", maxWeight, maxVolume));
        for (int matrixIndex : matrixIndexes) {
            route.addPoint(point(matrixIndex, "P" + matrixIndex, 100, 1));
        }
        RouteDistanceCalculator.updateTotalDistance(route, MATRIX);
        return route;
    }

    @Test
    void cheapestInsertionPicksCorrectPosition() {

        CheapestInsertion insertion = new CheapestInsertion();

        /*
         * ================= TRUONG HOP 1: chon dung cho re nhat =================
         *
         * Tuyen dang co: KHO -> A -> B -> KHO = 4000 + 3000 + 5000 = 12000 m
         * Can chen them C. Ba cho co the chen, tinh tay ca ba:
         *
         *   vi tri 0 (giua KHO va A): D[KHO][C] + D[C][A] - D[KHO][A]
         *                           = 3000     + 5000    - 4000      = 4000
         *   vi tri 1 (giua A va B)  : D[A][C]   + D[C][B] - D[A][B]
         *                           = 5000     + 4000    - 3000      = 6000
         *   vi tri 2 (giua B va KHO): D[B][C]   + D[C][KHO] - D[B][KHO]
         *                           = 4000     + 3000      - 5000    = 2000   <-- RE NHAT
         *
         * Vay phai chon vi tri 2, chi phi 2000 m. Tuyen moi la
         * KHO -> A -> B -> C -> KHO = 14000 m, dung bang 12000 + 2000.
         */
        OptimizationRoute route = routeWith(10_000, 100, A, B);
        assertEquals(12000, route.getTotalDistance(), 0.001);

        OptimizationPoint newPoint = point(C, "C", 100, 1);
        CheapestInsertion.InsertionCandidate candidate =
                insertion.findCheapestPosition(route, newPoint, -1, MATRIX);

        assertTrue(candidate.isFeasible(), "Xe con thua suc chua, phai chen duoc");
        assertEquals(2, candidate.position(),
                "Cho re nhat la vi tri 2 (giua B va kho), khong phai vi tri 0 hay 1");
        assertEquals(2000, candidate.extraDistance(), 0.001,
                "Chi phi chen = D[B][C] + D[C][KHO] - D[B][KHO] = 4000 + 3000 - 5000");

        // Chen that roi doi chieu tong quang duong moi
        assertTrue(insertion.insertCheapest(route, newPoint, -1, MATRIX));
        assertEquals(3, route.getPoints().size());
        assertEquals(14000, route.getTotalDistance(), 0.001,
                "Tuyen sau khi chen phai la 12000 + 2000 = 14000 m");
        assertEquals(300, route.getTotalWeight(), 0.001, "Chen xong phai cong don khoi luong");
        assertEquals(3, route.getTotalVolume(), 0.001, "Chen xong phai cong don the tich");

        /*
         * ================= TRUONG HOP 2: KHONG chen vao qua khu =================
         *
         * Day la buoc quan trong nhat cua ca thuat toan, va la cho de quen nhat.
         *
         * Tuyen dang co: KHO -> B -> A -> KHO = 5000 + 3000 + 4000 = 12000 m
         * Chen C, tinh tay ba cho:
         *
         *   vi tri 0 (giua KHO va B): D[KHO][C] + D[C][B] - D[KHO][B]
         *                           = 3000     + 4000    - 5000      = 2000   <-- RE NHAT
         *   vi tri 1 (giua B va A)  : D[B][C]   + D[C][A] - D[B][A]
         *                           = 4000     + 5000    - 3000      = 6000
         *   vi tri 2 (giua A va KHO): D[A][C]   + D[C][KHO] - D[A][KHO]
         *                           = 5000     + 3000      - 4000    = 4000
         *
         * Xe CON O KHO (lastVisitedIndex = -1) thi chon vi tri 0, chi phi 2000.
         */
        OptimizationRoute runningRoute = routeWith(10_000, 100, B, A);
        CheapestInsertion.InsertionCandidate atDepot =
                insertion.findCheapestPosition(runningRoute, point(C, "C", 100, 1), -1, MATRIX);
        assertEquals(0, atDepot.position(), "Xe con o kho thi duoc chen len dau tuyen");
        assertEquals(2000, atDepot.extraDistance(), 0.001);

        /*
         * Nhung neu xe DA GIAO XONG diem B (lastVisitedIndex = 0) thi vi tri 0 nam trong
         * qua khu — tai xe khong quay nguoc lai duoc. Chi con vi tri 1 (6000) va vi tri 2
         * (4000), nen dap an doi thanh vi tri 2 voi chi phi 4000.
         *
         * Bo buoc nay thi ham van tra ve 2000 va phuong an trong dep hon tren man hinh,
         * nhung tai xe khong thuc hien duoc.
         */
        CheapestInsertion.InsertionCandidate afterFirstStop =
                insertion.findCheapestPosition(runningRoute, point(C, "C", 100, 1), 0, MATRIX);
        assertEquals(2, afterFirstStop.position(),
                "Diem da giao roi thi khong duoc chen vao truoc no");
        assertEquals(4000, afterFirstStop.extraDistance(), 0.001,
                "Cho re nhat con lai la vi tri 2, chi phi 4000 m");

        /*
         * ================= TRUONG HOP 3: xe het cho =================
         *
         * Kiem CA HAI rang buoc rieng biet (CBR-03, CBR-04).
         */
        // Het TAI TRONG: xe 250 kg, dang cho 200 kg, them 100 kg nua la 300 > 250
        OptimizationRoute heavyRoute = routeWith(250, 100, A, B);
        assertFalse(insertion.findCheapestPosition(
                        heavyRoute, point(C, "C", 100, 1), -1, MATRIX).isFeasible(),
                "Vuot tai trong ma van bao chen duoc");

        // Het THE TICH: xe 2.5 m3, dang cho 2 m3, them 1 m3 nua la 3 > 2.5
        OptimizationRoute bulkyRoute = routeWith(10_000, 2.5, A, B);
        assertFalse(insertion.findCheapestPosition(
                        bulkyRoute, point(C, "C", 100, 1), -1, MATRIX).isFeasible(),
                "Vuot the tich ma van bao chen duoc — day la loi chi kiem mot rang buoc");

        // Va khong duoc lang le chen bua vao khi khong con cho
        assertFalse(insertion.insertCheapest(bulkyRoute, point(C, "C", 100, 1), -1, MATRIX));
        assertEquals(2, bulkyRoute.getPoints().size(), "Chen that bai ma van them diem vao tuyen");
    }
}
