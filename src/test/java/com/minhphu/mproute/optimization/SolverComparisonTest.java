package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.MetricsService;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RoutingResult;
import com.minhphu.mproute.service.optimization.heuristic.NN2OptSolver;
import com.minhphu.mproute.service.optimization.ortools.OrToolsSolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TEST BAT BUOC CO GIA TRI NHAT TRONG CA BO: hai bo giai chay tren cung du lieu phai
 * phu DUNG MOT TAP DIEM.
 *
 * Vi sao no gia tri nhat: no bat loi o CA HAI BEN. Ban tu cai dat sot mot diem ma
 * OR-Tools khong sot thi test do va chi thang vao cho sai — khong phai doi den luc demo
 * moi phat hien mot khach hang bi bo quen.
 *
 * Va no con giu cho bang so sanh chuong 4 co nghia: hai con so km chi dat canh nhau
 * duoc khi hai ben phu cung tap diem. Ben nao bo bot diem thi tuyen ngan hon la duong
 * nhien, khong phai vi thuat toan tot hon.
 */
class SolverComparisonTest {

    /*
     * BAY DIEM TREN MAT PHANG (don vi km), kho o goc toa do:
     *
     *                      B(3,3)
     *          C(1,4)
     *     D(-2,3)
     *                            A(2,1)
     *     E(-3,1)
     *  ----------------- KHO(0,0) -----------------
     *          F(-1,-2)
     *
     * Khoang cach lay theo duong chim bay Euclid tren mat phang, doi xung san nen 2-opt
     * dung duoc ngay (khong nhu ma tran OSRM tho, phai doi xung hoa truoc).
     *
     * Chon hinh nay chu khong chon duong thang: tren duong thang hai bo giai deu ra cung
     * mot dap so nen test khong chung minh duoc gi. O day hai ben ra thu tu khac nhau,
     * ma van phai phu dung mot tap diem.
     */
    private static final double[][] COORDINATES_KM = {
            /* KHO */ { 0,  0},
            /* A   */ { 2,  1},
            /* B   */ { 3,  3},
            /* C   */ { 1,  4},
            /* D   */ {-2,  3},
            /* E   */ {-3,  1},
            /* F   */ {-1, -2}
    };

    private static final String[] NAMES = {"KHO", "A", "B", "C", "D", "E", "F"};

    private static final double[][] MATRIX = buildEuclideanMatrix(COORDINATES_KM);

    private static double[][] buildEuclideanMatrix(double[][] coordinatesKm) {
        int size = coordinatesKm.length;
        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double deltaX = coordinatesKm[i][0] - coordinatesKm[j][0];
                double deltaY = coordinatesKm[i][1] - coordinatesKm[j][1];
                matrix[i][j] = Math.sqrt(deltaX * deltaX + deltaY * deltaY) * 1000;
            }
        }
        return matrix;
    }

    /** Sau diem giao, moi diem 100 kg / 1.0 m3. Kho la chi so 0, khong nam trong danh sach. */
    private List<OptimizationPoint> deliveryPoints() {
        List<OptimizationPoint> points = new ArrayList<>();
        for (int matrixIndex = 1; matrixIndex < COORDINATES_KM.length; matrixIndex++) {
            OptimizationPoint point = OptimizationPoint.forOrder(
                    (long) matrixIndex, NAMES[matrixIndex],
                    COORDINATES_KM[matrixIndex][0], COORDINATES_KM[matrixIndex][1],
                    100, 1.0);
            point.setMatrixIndex(matrixIndex);
            points.add(point);
        }
        return points;
    }

    private Set<Long> coveredOrderIds(RoutingResult result) {
        Set<Long> orderIds = new LinkedHashSet<>();
        for (OptimizationRoute route : result.getRoutes()) {
            for (OptimizationPoint point : route.getPoints()) {
                orderIds.add(point.getOrderId());
            }
        }
        return orderIds;
    }

    @Test
    void bothSolversCoverTheSamePointSet() {

        List<OptimizationPoint> points = deliveryPoints();

        /*
         * Suc chua CO Y de rong rai: 2 xe x 1000 kg / 10 m3, trong khi tong nhu cau chi
         * 600 kg / 6 m3. Khong ben nao co ly do chinh dang de bo diem, nen ca hai BAT
         * BUOC phai phu het sau diem.
         */
        List<OptimizationVehicle> vehicles = List.of(
                new OptimizationVehicle(1L, "29H-123.45", 1000, 10.0),
                new OptimizationVehicle(2L, "29C-678.90", 1000, 10.0));

        /*
         * Moi ben nhan mot BAN SAO danh sach: NearestNeighborSolver xoa dan phan tu khoi
         * danh sach no dang chay. Dung chung mot danh sach thi bo giai chay sau nhan dau
         * vao da bi bo giai truoc dong vao — va test se do vi mot ly do khong lien quan
         * gi den thuat toan.
         */
        RoutingResult heuristicResult =
                new NN2OptSolver().solve(new ArrayList<>(points), vehicles, MATRIX);
        RoutingResult orToolsResult =
                new OrToolsSolver(1).solve(new ArrayList<>(points), vehicles, MATRIX);

        // ---------- 1. Khong ben nao duoc bo sot diem ----------
        assertTrue(heuristicResult.getUnassignedPoints().isEmpty(),
                "NN+2-opt bo sot diem trong khi suc chua thua: "
                        + heuristicResult.getUnassignedPoints());
        assertTrue(orToolsResult.getUnassignedPoints().isEmpty(),
                "OR-Tools bo sot diem trong khi suc chua thua: "
                        + orToolsResult.getUnassignedPoints());

        // ---------- 2. Hai ben phu DUNG MOT TAP DIEM ----------
        Set<Long> heuristicCoverage = coveredOrderIds(heuristicResult);
        Set<Long> orToolsCoverage = coveredOrderIds(orToolsResult);

        assertEquals(orToolsCoverage, heuristicCoverage,
                "Hai bo giai phu tap diem khac nhau — mot trong hai ben dang lam sai");

        Set<Long> expectedCoverage = new LinkedHashSet<>(List.of(1L, 2L, 3L, 4L, 5L, 6L));
        assertEquals(expectedCoverage, orToolsCoverage,
                "Tap diem duoc phu phai dung bang tap diem dau vao");

        // ---------- 3. Khong ben nao duoc GIAO TRUNG mot diem cho hai xe ----------
        /*
         * Dem so luot xuat hien phai bang so diem. Set o tren khong bat duoc loi nay:
         * mot diem bi xep vao ca hai tuyen thi tap hop van dung, chi co so luot la thua.
         * Loi kieu do lam tong quang duong phong len va khach nhan hang hai lan.
         */
        assertEquals(points.size(), heuristicResult.assignedPointCount(),
                "NN+2-opt giao trung mot diem cho nhieu xe");
        assertEquals(points.size(), orToolsResult.assignedPointCount(),
                "OR-Tools giao trung mot diem cho nhieu xe");

        // ---------- 4. Ca hai ben deu ton trong suc chua ----------
        for (OptimizationRoute route : heuristicResult.getRoutes()) {
            assertTrue(route.getTotalWeight() <= route.getVehicle().getMaxWeight() + 1e-9);
            assertTrue(route.getTotalVolume() <= route.getVehicle().getMaxVolume() + 1e-9);
        }
        for (OptimizationRoute route : orToolsResult.getRoutes()) {
            assertTrue(route.getTotalWeight() <= route.getVehicle().getMaxWeight() + 1e-9);
            assertTrue(route.getTotalVolume() <= route.getVehicle().getMaxVolume() + 1e-9);
        }

        /*
         * ---------- 5. Cung tap diem thi moi so km duoc, va OR-Tools phai khong thua ----------
         *
         * Day la con so cua chuong 4: bo giai thu vien phai it nhat bang ban tu cai dat.
         * Neu mot ngay nao do test nay do theo chieu nguoc lai thi khong phai la OR-Tools
         * kem di, ma gan nhu chac chan la mot trong hai ben dang tinh quang duong sai —
         * ca hai deu goi chung RouteDistanceCalculator nen chung khong the lech thuoc do.
         */
        assertTrue(orToolsResult.getOptimizedDistance() <= heuristicResult.getOptimizedDistance() + 1e-6,
                "OR-Tools ra tuyen DAI HON ban tu cai dat: "
                        + orToolsResult.getOptimizedDistance() + " m so voi "
                        + heuristicResult.getOptimizedDistance() + " m");

        /*
         * ---------- 6. Hai moc so sanh phai theo dung thu tu y nghia ----------
         *
         * Moc A (moi don mot chuyen khu hoi rieng) la cach lam kem nhat, phai dai hon
         * moc B (dung cach chia xe, chi sai thu tu), va moc B phai dai hon hoac bang
         * ket qua toi uu. Thu tu nay ma dao lon thi la MetricsService tinh sai.
         */
        MetricsService.fillBaselines(orToolsResult, MATRIX);
        assertTrue(orToolsResult.getBaselineADistance() > orToolsResult.getBaselineBDistance(),
                "Moc A phai dai hon moc B");
        assertTrue(orToolsResult.getBaselineBDistance() >= orToolsResult.getOptimizedDistance() - 1e-6,
                "Moc B phai dai hon hoac bang ket qua da toi uu thu tu");
        assertTrue(MetricsService.reductionPercent(
                        orToolsResult.getBaselineADistance(), orToolsResult.getOptimizedDistance()) > 0,
                "Muc giam so voi moc A phai la so duong");
    }
}
