package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.service.optimization.heuristic.NearestNeighborSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chay thu thuat toan bang console — KHONG can khoi dong web, KHONG can MySQL,
 * KHONG can OSRM.
 *
 *     mvnw.cmd compile exec:java -Dexec.mainClass=com.minhphu.mproute.service.optimization.AlgorithmRunner
 *     (hoac bam Run thang trong IDE)
 *
 * Lop nay lam ba viec, ca ba deu can den het do an:
 *   1. Go loi nhanh o tuan 6-7: tach bach loi THUAT TOAN voi loi Spring / JPA / giao dien.
 *      Tuyen ra sai o day thi chac chan la thuat toan sai, khong phai tai cai khac.
 *   2. Chay lai duoc dung bo du lieu demo cua docs/08 de doi chieu voi ket qua da ghi.
 *   3. Tuan 10 dung lai chinh lop nay de chay benchmark Augerat.
 *
 * Du lieu duoi day CHEP TU docs/08-du-lieu-mau.md — 8 don cua ngay mai va 3 xe.
 * Khoang cach dung Haversine x 1.3 (khong can OSRM), nen con so km o day la XAP XI;
 * muon so that thi chay qua man hinh phan tuyen voi OSRM bat len.
 */
public final class AlgorithmRunner {

    // Kho Minh Phu, duong Phan Trong Tue, Thanh Tri (docs/08)
    private static final double DEPOT_LATITUDE = 20.9497;
    private static final double DEPOT_LONGITUDE = 105.8172;

    private static final double HAVERSINE_DETOUR_FACTOR = 1.3;

    private AlgorithmRunner() {
    }

    public static void main(String[] args) {

        List<OptimizationPoint> allPoints = sampleDeliveryPoints();
        List<OptimizationVehicle> vehicles = sampleVehicles();

        /*
         * Nap ma tran MOT LAN truoc khi chay thuat toan. Day la diem mau chot ve hieu nang:
         * sau dong nay, thuat toan chi tra matrix[i][j] chu khong goi lai ham tinh khoang
         * cach nua (dieu cam so 4 trong quy uoc ky thuat).
         */
        double[][] matrix = DistanceCalculator.buildMatrix(
                allPoints, new HaversineDistanceCalculator(HAVERSINE_DETOUR_FACTOR));

        printInput(allPoints, vehicles);

        // Diem giao = toan bo danh sach TRU kho o dau
        List<OptimizationPoint> deliveryPoints = allPoints.subList(1, allPoints.size());

        RoutingResult result = new NearestNeighborSolver().solve(deliveryPoints, vehicles, matrix);

        printResult(result, matrix);
    }

    // ==================== In ket qua ====================

    private static void printInput(List<OptimizationPoint> allPoints,
                                   List<OptimizationVehicle> vehicles) {
        double totalWeight = 0;
        double totalVolume = 0;
        for (OptimizationPoint point : allPoints) {
            totalWeight += point.getWeight();
            totalVolume += point.getVolume();
        }

        double capacityWeight = 0;
        double capacityVolume = 0;
        for (OptimizationVehicle vehicle : vehicles) {
            capacityWeight += vehicle.getMaxWeight();
            capacityVolume += vehicle.getMaxVolume();
        }

        System.out.println("=".repeat(78));
        System.out.println("CHAY THU NEAREST NEIGHBOR - du lieu mau docs/08, khoang cach Haversine x "
                + HAVERSINE_DETOUR_FACTOR);
        System.out.println("=".repeat(78));
        System.out.printf(Locale.ROOT, "Nhu cau : %d diem giao, %.1f kg, %.3f m3%n",
                allPoints.size() - 1, totalWeight, totalVolume);
        System.out.printf(Locale.ROOT, "Suc chua: %d xe, %.1f kg, %.3f m3%n",
                vehicles.size(), capacityWeight, capacityVolume);
        for (OptimizationVehicle vehicle : vehicles) {
            System.out.println("          " + vehicle);
        }
        System.out.println();
    }

    private static void printResult(RoutingResult result, double[][] matrix) {
        System.out.println("-".repeat(78));
        System.out.println("KET QUA");
        System.out.println("-".repeat(78));

        int routeNumber = 1;
        for (OptimizationRoute route : result.getRoutes()) {
            System.out.printf(Locale.ROOT, "Tuyen %d - %s%n", routeNumber++,
                    route.getVehicle().getLicensePlate());
            System.out.printf(Locale.ROOT,
                    "   %.2f km | %.1f/%.0f kg (%.0f%%) | %.3f/%.1f m3 (%.0f%%)%n",
                    route.getTotalDistance() / 1000.0,
                    route.getTotalWeight(), route.getVehicle().getMaxWeight(),
                    route.weightUtilization() * 100,
                    route.getTotalVolume(), route.getVehicle().getMaxVolume(),
                    route.volumeUtilization() * 100);

            // In tung chang de kiem tay duoc, ke ca CHANG CUOI VE KHO
            int previousIndex = RouteDistanceCalculator.DEPOT_INDEX;
            int stopNumber = 1;
            for (OptimizationPoint point : route.getPoints()) {
                double legMeters = matrix[previousIndex][point.getMatrixIndex()];
                System.out.printf(Locale.ROOT, "     %d. %-22s %6.2f km  (%.1f kg, %.3f m3)%n",
                        stopNumber++, point.getName(), legMeters / 1000.0,
                        point.getWeight(), point.getVolume());
                previousIndex = point.getMatrixIndex();
            }
            double returnLegMeters = matrix[previousIndex][RouteDistanceCalculator.DEPOT_INDEX];
            System.out.printf(Locale.ROOT, "        %-22s %6.2f km  <-- chang cuoi ve kho (CBR-01)%n",
                    "KHO", returnLegMeters / 1000.0);
            System.out.println();
        }

        if (result.hasUnassignedPoints()) {
            System.out.println("KHONG GAN DUOC - tong suc chua co the van du, nhung phan don cho xe");
            System.out.println("la bai toan xep thung nen thuat toan tham lam khong chia het (docs/08):");
            for (OptimizationPoint point : result.getUnassignedPoints()) {
                System.out.printf(Locale.ROOT, "     %-22s %.1f kg, %.3f m3%n",
                        point.getName(), point.getWeight(), point.getVolume());
            }
            System.out.println();
        }

        System.out.printf(Locale.ROOT, "Tong: %d tuyen, %d/%d diem duoc gan, %.2f km, chay het %d ms%n",
                result.getRoutes().size(),
                result.assignedPointCount(),
                result.assignedPointCount() + result.getUnassignedPoints().size(),
                result.getOptimizedDistance() / 1000.0,
                result.getExecutionTimeMs());
    }

    // ==================== Du lieu cung, chep tu docs/08 ====================

    /**
     * Kho o dau danh sach (chi so 0), roi 8 don cua ngay mai.
     * Toa do va nhu cau chep nguyen tu docs/08-du-lieu-mau.md.
     */
    private static List<OptimizationPoint> sampleDeliveryPoints() {
        List<OptimizationPoint> points = new ArrayList<>();

        points.add(OptimizationPoint.depot(DEPOT_LATITUDE, DEPOT_LONGITUDE));

        points.add(OptimizationPoint.forOrder(1L, "Tap hoa Minh Anh", 21.0148, 105.8125, 300.0, 4.250));
        points.add(OptimizationPoint.forOrder(2L, "Sieu thi Hong Ha", 21.0106, 105.8215, 727.5, 1.920));
        points.add(OptimizationPoint.forOrder(3L, "Cua hang Bao Chau", 21.0067, 105.8290, 372.0, 0.465));
        points.add(OptimizationPoint.forOrder(4L, "Dai ly Thanh Tam", 21.0102, 105.7963, 240.0, 3.400));
        points.add(OptimizationPoint.forOrder(5L, "Tap hoa Duc Loi", 20.9955, 105.8055, 110.0, 1.440));
        points.add(OptimizationPoint.forOrder(6L, "Cua hang Kim Ngan", 21.0025, 105.8010, 107.5, 0.140));
        points.add(OptimizationPoint.forOrder(7L, "Sieu thi An Phat", 20.9895, 105.8412, 355.0, 0.600));
        points.add(OptimizationPoint.forOrder(8L, "Tap hoa Binh Minh", 20.9820, 105.8450, 428.0, 2.860));

        return points;
    }

    /**
     * Ba xe AVAILABLE cua docs/08. Xe thu tu 29B-222.11 dang MAINTENANCE nen khong
     * duoc dua vao phan tuyen (CBR-05) — chinh la kich ban demo giai doan 2.
     */
    private static List<OptimizationVehicle> sampleVehicles() {
        List<OptimizationVehicle> vehicles = new ArrayList<>();
        vehicles.add(new OptimizationVehicle(1L, "29H-123.45", 1500, 12.0));
        vehicles.add(new OptimizationVehicle(2L, "29C-678.90", 500, 6.0));
        vehicles.add(new OptimizationVehicle(3L, "29F-555.66", 1000, 8.0));
        return vehicles;
    }
}
