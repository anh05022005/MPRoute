package com.minhphu.mproute.optimization;

import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RoutingResult;
import com.minhphu.mproute.service.optimization.ortools.OrToolsSolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hai test bat buoc cho bo giai chinh OrToolsSolver.
 *
 * Test thu ba cua bo giai chinh — orToolsSolvesSmallCvrpToHandCalculatedOptimum — nam o
 * OrToolsSmokeTest, viet truoc de kiem chung thu vien chay duoc tren may nay. Test thu tu
 * — bothSolversCoverTheSamePointSet — nam o SolverComparisonTest vi no dung toi ca hai
 * bo giai.
 *
 * Gioi han thoi gian dat 1 giay thay vi 5 giay mac dinh: cac bai o day chi 4 diem, mot
 * giay la thua, va bo test khong nen ngoi cho.
 */
class OrToolsSolverTest {

    /*
     * NAM DIEM TREN MOT DUONG THANG, don vi km:
     *
     *      KHO(0)   A(1)   B(2)   C(3)   D(4)
     *
     * Khoang cach = hieu toa do. Chon hinh nay vi moi phuong an deu cong tay ra duoc.
     *
     * ===================== HAI DAP SO TINH TAY, DUNG XUYEN SUOT FILE =====================
     *
     * (1) MOT XE cho het bon diem:
     *     KHO -> A -> B -> C -> D -> KHO = 1 + 1 + 1 + 1 + 4 = 8 km = 8000 m
     *
     * (2) HAI XE, moi xe dung hai diem — ba cach chia:
     *     {A,B} va {C,D}: (1+1+2) + (3+1+4) = 4000 + 8000 = 12000 m   <-- tot nhat
     *     {A,C} va {B,D}: (1+2+3) + (2+2+4) = 6000 + 8000 = 14000 m
     *     {A,D} va {B,C}: (1+3+4) + (2+1+3) = 8000 + 6000 = 14000 m
     *
     * DIEM MAU CHOT: phuong an mot xe (8000 m) NGAN HON HAN phuong an hai xe (12000 m).
     * Nen neu mot rang buoc suc chua bi khai thieu, bo giai se chon phuong an mot xe va
     * test do ngay. Do la cach test nay chung minh ca hai Dimension that su co hieu luc,
     * chu khong phai chi doc lai con so bo giai tu bao.
     */
    private static final double[][] MATRIX = buildLineMatrix(new double[]{0, 1, 2, 3, 4});

    private static final double SINGLE_VEHICLE_OPTIMUM = 8000;
    private static final double TWO_VEHICLE_OPTIMUM = 12000;

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

    private List<OptimizationPoint> fourPoints(double weightEach, double volumeEach) {
        List<OptimizationPoint> points = new ArrayList<>();
        String[] names = {"A", "B", "C", "D"};
        for (int matrixIndex = 1; matrixIndex <= 4; matrixIndex++) {
            OptimizationPoint point = OptimizationPoint.forOrder(
                    (long) matrixIndex, names[matrixIndex - 1], 0, 0, weightEach, volumeEach);
            point.setMatrixIndex(matrixIndex);
            points.add(point);
        }
        return points;
    }

    private List<OptimizationVehicle> vehicles(int count, double maxWeight, double maxVolume) {
        List<OptimizationVehicle> vehicles = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            vehicles.add(new OptimizationVehicle((long) i, "29H-000.0" + i, maxWeight, maxVolume));
        }
        return vehicles;
    }

    /**
     * TEST BAT BUOC: ca hai Dimension deu co hieu luc (CBR-03, CBR-04).
     *
     * VI SAO TEST NAY QUAN TRONG: gan nhu moi vi du OR-Tools tren mang chi khai MOT
     * Dimension ten "Capacity". Chep ve dung thi he thong chay binh thuong, ket qua trong
     * hop ly, chi co xe cho toan bong tay trang la day thung ma phan mem van tuong con
     * cho. Loi kieu do khong lo ra khi demo, no lo ra khi doanh nghiep dung that.
     *
     * Test kiem HAI CHIEU RIENG BIET: mot lan de the tich la thu duy nhat chan, mot lan
     * de khoi luong la thu duy nhat chan. Moi lan, chieu con lai deu du rong de neu
     * Dimension kia bi thieu thi bo giai se chon phuong an mot xe 8000 m.
     */
    @Test
    void orToolsRespectsBothCapacityDimensions() {

        OrToolsSolver solver = new OrToolsSolver(1);

        /*
         * ============ CHIEU 1: THE TICH la rang buoc chan ============
         *
         * Moi diem 100 kg / 4.0 m3. Hai xe, moi xe 1000 kg / 9.0 m3.
         *
         *   Khoi luong: 4 x 100 = 400 kg, con xa 1000 kg  -> KHONG chan gi
         *   The tich  : 9.0 / 4.0 = 2.25                  -> moi xe toi da HAI diem
         *
         * Vay dap so phai la 12000 m voi hai tuyen, moi tuyen hai diem.
         * Neu Dimension "Volume" bi thieu thi ket qua se la 8000 m mot tuyen bon diem.
         */
        List<OptimizationPoint> bulkyPoints = fourPoints(100, 4.0);
        RoutingResult bulkyResult = solver.solve(bulkyPoints, vehicles(2, 1000, 9.0), MATRIX);

        assertEquals(AlgorithmType.OR_TOOLS, bulkyResult.getAlgorithm());
        assertEquals(4, bulkyResult.assignedPointCount(), "Phai xep het bon diem");
        assertTrue(bulkyResult.getUnassignedPoints().isEmpty());
        assertEquals(2, bulkyResult.getRoutes().size(),
                "The tich chi cho hai diem moi xe, nen bat buoc phai dung hai tuyen. "
                        + "Ra mot tuyen nghia la Dimension the tich khong co hieu luc");
        assertEquals(TWO_VEHICLE_OPTIMUM, bulkyResult.getOptimizedDistance(), 1.0,
                "Dap so tinh tay cua phuong an hai xe la 12000 m");
        assertTrue(bulkyResult.getOptimizedDistance() > SINGLE_VEHICLE_OPTIMUM,
                "Ra 8000 m nghia la bo giai da don het len mot xe, tuc la vuot the tich");

        for (OptimizationRoute route : bulkyResult.getRoutes()) {
            assertTrue(route.getTotalVolume() <= route.getVehicle().getMaxVolume() + 1e-9,
                    "Tuyen vuot the tich thung xe: " + route.getTotalVolume()
                            + " m3 > " + route.getVehicle().getMaxVolume() + " m3");
            assertTrue(route.getTotalWeight() <= route.getVehicle().getMaxWeight() + 1e-9,
                    "Tuyen vuot tai trong: " + route.getTotalWeight() + " kg");
            assertEquals(2, route.getPoints().size(), "Moi tuyen chi duoc cho dung hai diem");
        }

        /*
         * ============ CHIEU 2: KHOI LUONG la rang buoc chan ============
         *
         * Moi diem 400 kg / 0.1 m3. Hai xe, moi xe 1000 kg / 10.0 m3.
         *
         *   The tich  : 4 x 0.1 = 0.4 m3, con xa 10 m3  -> KHONG chan gi
         *   Khoi luong: 1000 / 400 = 2.5                -> moi xe toi da HAI diem
         *
         * Dap so van la 12000 m, hai tuyen. Lan nay neu thieu Dimension "Weight" thi
         * moi ra 8000 m.
         */
        List<OptimizationPoint> heavyPoints = fourPoints(400, 0.1);
        RoutingResult heavyResult = solver.solve(heavyPoints, vehicles(2, 1000, 10.0), MATRIX);

        assertEquals(4, heavyResult.assignedPointCount(), "Phai xep het bon diem");
        assertEquals(2, heavyResult.getRoutes().size(),
                "Tai trong chi cho hai diem moi xe, nen bat buoc phai dung hai tuyen. "
                        + "Ra mot tuyen nghia la Dimension khoi luong khong co hieu luc");
        assertEquals(TWO_VEHICLE_OPTIMUM, heavyResult.getOptimizedDistance(), 1.0);

        for (OptimizationRoute route : heavyResult.getRoutes()) {
            assertTrue(route.getTotalWeight() <= route.getVehicle().getMaxWeight() + 1e-9,
                    "Tuyen vuot tai trong: " + route.getTotalWeight()
                            + " kg > " + route.getVehicle().getMaxWeight() + " kg");
            assertEquals(2, route.getPoints().size(), "Moi tuyen chi duoc cho dung hai diem");
        }
    }

    /**
     * TEST BAT BUOC: khong du suc chua thi tra ve DANH SACH diem khong xep duoc,
     * khong tra ve null va cung khong lang le bo qua.
     *
     * DAY LA LY DO PHAI KHAI addDisjunction. Mac dinh OR-Tools bat moi diem phai duoc
     * tham; tong suc chua khong du thi solveWithParameters() tra ve null, va null thi
     * man hinh phan tuyen khong phan biet duoc "thieu suc chua" voi "thu vien loi" —
     * chi bao duoc mot loi chung chung, dieu phoi vien khong biet phai lam gi.
     *
     * Khai addDisjunction bien viec bo mot diem thanh mot lua chon CO GIA, bo giai chi
     * bo khi that su het cach. Cac diem bi bo doc ra duoc va do vao unassignedPoints de
     * hien thong bao MSG-08 kem ten khach hang cu the (CN-03-14).
     */
    @Test
    void orToolsDropsPointsWhenCapacityIsInsufficient() {

        /*
         * Bon diem, moi diem 400 kg. CHI MOT XE 1000 kg.
         *   1000 / 400 = 2.5  ->  xe cho duoc nhieu nhat HAI diem
         *   Con lai hai diem khong co xe nao nhan.
         */
        List<OptimizationPoint> points = fourPoints(400, 0.1);
        RoutingResult result = new OrToolsSolver(1)
                .solve(points, vehicles(1, 1000, 10.0), MATRIX);

        assertNotNull(result.getUnassignedPoints(),
                "unassignedPoints phai la danh sach RONG chu khong bao gio null");
        assertTrue(result.hasUnassignedPoints(),
                "Thieu suc chua ma bo giai bao xep het — addDisjunction khong chay dung");

        assertEquals(2, result.getUnassignedPoints().size(),
                "Xe cho duoc hai diem, nen dung hai diem bi bo lai");
        assertEquals(2, result.assignedPointCount(), "Hai diem con lai phai duoc xep");

        // Khong duoc lam BOC HOI diem nao: gan + khong gan phai bang tong dau vao
        assertEquals(points.size(),
                result.assignedPointCount() + result.getUnassignedPoints().size(),
                "Tong so diem vao va ra khong khop — co diem bi mat hut");

        // Phan xep duoc van phai ton trong suc chua
        for (OptimizationRoute route : result.getRoutes()) {
            assertTrue(route.getTotalWeight() <= route.getVehicle().getMaxWeight() + 1e-9,
                    "Bo diem roi ma phan con lai van vuot tai trong");
        }

        /*
         * Xe se giu lai hai diem RE NHAT la A va B:
         *     KHO -> A -> B -> KHO = 1 + 1 + 2 = 4 km = 4000 m
         * Moi cap khac deu dai hon, nen day la dap so tinh tay duy nhat.
         */
        assertEquals(4000, result.getOptimizedDistance(), 1.0,
                "Xe phai giu lai hai diem gan kho nhat: KHO->A->B->KHO = 4000 m");
    }
}
