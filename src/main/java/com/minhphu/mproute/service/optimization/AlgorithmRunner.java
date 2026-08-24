package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.service.optimization.heuristic.NN2OptSolver;
import com.minhphu.mproute.service.optimization.heuristic.NearestNeighborSolver;
import com.minhphu.mproute.service.optimization.ortools.OrToolsSolver;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Chay thu thuat toan bang console — KHONG can khoi dong web, KHONG can MySQL,
 * KHONG can OSRM.
 *
 * CACH CHAY — bam Run thang trong IDE la de nhat. Tu dong lenh thi lam hai buoc:
 *
 *     mvnw.cmd compile
 *     mvnw.cmd dependency:build-classpath -Dmdep.outputFile=cp.txt
 *     java -cp "target/classes;%cd%\cp.txt-noi-dung" com...AlgorithmRunner        (mac dinh 8 diem)
 *     java -cp "..."                                 com...AlgorithmRunner 30     (sinh 30 diem)
 *
 * KHONG dung duoc "mvnw.cmd exec:java". exec-maven-plugin nap lop bang classloader
 * rieng cua no, va classloader do khong tim thay thu muc tai nguyen chua thu vien
 * native cua OR-Tools:
 *
 *     Resource ortools-win32-x86-64/ was not found in ClassLoader ...ExecJavaClassLoader
 *
 * Loi nay chi hien ra khi chay toi OrToolsSolver, tuc la sau khi da in xong phan dau,
 * nen rat de tuong la loi thuat toan. IDE va lenh "java -cp" o tren deu chay binh thuong
 * vi chung dung classloader chuan.
 *
 * Lop nay lam ba viec, ca ba deu can den het do an:
 *   1. Go loi nhanh o tuan 6-7: tach bach loi THUAT TOAN voi loi Spring / JPA / giao dien.
 *      Tuyen ra sai o day thi chac chan la thuat toan sai, khong phai tai cai khac.
 *   2. In BANG SO SANH BON MOC — chinh la bang so lieu cua chuong 4 bao cao.
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

    // Hai tham so he thong, mac dinh o docs/02: AVERAGE_SPEED va LOADING_TIME
    private static final double AVERAGE_SPEED_KMH = 30.0;
    private static final int LOADING_MINUTES_PER_STOP = 15;

    private static final LocalTime DEPARTURE_TIME = LocalTime.of(8, 0);

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private AlgorithmRunner() {
    }

    /**
     * Khong tham so thi chay bo du lieu mau 8 don cua docs/08.
     * Truyen mot so thi sinh bo du lieu lon hon de do hieu nang va do muc giam:
     *
     *     java ... AlgorithmRunner 30
     */
    public static void main(String[] args) {

        int generatedPointCount = readPointCount(args);

        List<OptimizationPoint> allPoints;
        List<OptimizationVehicle> vehicles;
        String datasetLabel;
        if (generatedPointCount > 0) {
            allPoints = generatedDeliveryPoints(generatedPointCount);
            vehicles = fleetFor(allPoints);
            datasetLabel = generatedPointCount + " diem sinh voi hat giong co dinh";
        } else {
            allPoints = sampleDeliveryPoints();
            vehicles = sampleVehicles();
            datasetLabel = "du lieu mau docs/08";
        }

        /*
         * Nap ma tran MOT LAN truoc khi chay thuat toan. Day la diem mau chot ve hieu nang:
         * sau dong nay, thuat toan chi tra matrix[i][j] chu khong goi lai ham tinh khoang
         * cach nua (dieu cam so 4 trong quy uoc ky thuat).
         *
         * Ma tran Haversine von da doi xung san, nen 2-opt dung duoc ngay. Ma tran OSRM
         * that thi phai qua buoc doi xung hoa cua DistanceMatrixService truoc.
         */
        double[][] matrix = DistanceCalculator.buildMatrix(
                allPoints, new HaversineDistanceCalculator(HAVERSINE_DETOUR_FACTOR));

        printInput(datasetLabel, allPoints, vehicles);

        // Diem giao = toan bo danh sach TRU kho o dau
        List<OptimizationPoint> deliveryPoints = allPoints.subList(1, allPoints.size());

        /*
         * Chay ba bo giai tren CUNG mot ma tran va CUNG doi xe. Moi ben nhan mot ban sao
         * danh sach diem — cac bo giai ghi thu tu diem vao tuyen cua rieng minh.
         */
        RoutingResult nearestNeighborResult =
                new NearestNeighborSolver().solve(new ArrayList<>(deliveryPoints), vehicles, matrix);
        RoutingResult heuristicResult =
                new NN2OptSolver().solve(new ArrayList<>(deliveryPoints), vehicles, matrix);
        RoutingResult orToolsResult =
                new OrToolsSolver().solve(new ArrayList<>(deliveryPoints), vehicles, matrix);

        MetricsService.fillBaselines(orToolsResult, matrix);

        // Gio du kien den tung diem, tinh tren tuyen ma he thong thuc su dung (CBR-13)
        LocalDateTime departure = LocalDateTime.of(java.time.LocalDate.now(), DEPARTURE_TIME);
        EtaCalculator.assignEta(orToolsResult.getRoutes(), matrix, departure,
                AVERAGE_SPEED_KMH, LOADING_MINUTES_PER_STOP);

        printRoutes("KET QUA OR-TOOLS (bo giai chinh - he thong chay bang cai nay)",
                orToolsResult, matrix);

        printComparisonTable(nearestNeighborResult, heuristicResult, orToolsResult, matrix);
    }

    // ==================== In ket qua ====================

    private static void printInput(String datasetLabel, List<OptimizationPoint> allPoints,
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
        System.out.println("CHAY THU THUAT TOAN - " + datasetLabel
                + ", khoang cach Haversine x " + HAVERSINE_DETOUR_FACTOR);
        System.out.println("=".repeat(78));
        System.out.printf(Locale.ROOT, "Nhu cau : %d diem giao, %.1f kg, %.3f m3%n",
                allPoints.size() - 1, totalWeight, totalVolume);
        System.out.printf(Locale.ROOT, "Suc chua: %d xe, %.1f kg, %.3f m3%n",
                vehicles.size(), capacityWeight, capacityVolume);
        for (OptimizationVehicle vehicle : vehicles) {
            System.out.println("          " + vehicle);
        }
        System.out.printf(Locale.ROOT, "Xuat phat %s, toc do %.0f km/h, boc do %d phut moi diem%n%n",
                DEPARTURE_TIME.format(HOUR_MINUTE), AVERAGE_SPEED_KMH, LOADING_MINUTES_PER_STOP);
    }

    private static void printRoutes(String title, RoutingResult result, double[][] matrix) {
        System.out.println("-".repeat(78));
        System.out.println(title);
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
                String eta = point.getEstimatedArrival() == null
                        ? "  --  "
                        : point.getEstimatedArrival().format(HOUR_MINUTE);
                System.out.printf(Locale.ROOT, "     %d. %-22s %6.2f km  %s  (%.1f kg, %.3f m3)%n",
                        stopNumber++, point.getName(), legMeters / 1000.0, eta,
                        point.getWeight(), point.getVolume());
                previousIndex = point.getMatrixIndex();
            }

            double returnLegMeters = matrix[previousIndex][RouteDistanceCalculator.DEPOT_INDEX];
            LocalDateTime backAtDepot = EtaCalculator.estimatedReturnToDepot(
                    route, matrix, AVERAGE_SPEED_KMH, LOADING_MINUTES_PER_STOP);
            System.out.printf(Locale.ROOT,
                    "        %-22s %6.2f km  %s  <-- chang cuoi ve kho (CBR-01)%n",
                    "KHO", returnLegMeters / 1000.0,
                    backAtDepot == null ? "  --  " : backAtDepot.format(HOUR_MINUTE));
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

        System.out.printf(Locale.ROOT,
                "Tong: %d tuyen, %d/%d diem duoc gan, %.2f km, lap day TB %.0f%% kg / %.0f%% m3%n%n",
                result.getRoutes().size(),
                result.assignedPointCount(),
                result.assignedPointCount() + result.getUnassignedPoints().size(),
                result.getOptimizedDistance() / 1000.0,
                MetricsService.averageWeightUtilization(result.getRoutes()) * 100,
                MetricsService.averageVolumeUtilization(result.getRoutes()) * 100);
    }

    /**
     * Bang so lieu cua chuong 4 bao cao.
     *
     * Cot "giam" deu lay MOC A lam goc, vi moc A la cach Minh Phu dang lam that — do la
     * con so tra loi cau "de tai giai quyet duoc gi". Rieng dong OR-Tools in them khoang
     * cach giua ban tu cai dat va bo giai thu vien.
     */
    private static void printComparisonTable(RoutingResult nearestNeighborResult,
                                             RoutingResult heuristicResult,
                                             RoutingResult orToolsResult,
                                             double[][] matrix) {

        double baselineA = orToolsResult.getBaselineADistance();
        double baselineB = orToolsResult.getBaselineBDistance();
        int fullPointCount = orToolsResult.assignedPointCount()
                + orToolsResult.getUnassignedPoints().size();

        System.out.println("=".repeat(78));
        System.out.println("BANG SO SANH - tat ca chay tren CUNG mot ma tran va CUNG doi xe");
        System.out.println("=".repeat(78));
        System.out.printf(Locale.ROOT, "%-38s %5s %10s %8s %9s%n",
                "Cach phan tuyen", "Diem", "Km", "Giam", "Thoi gian");
        System.out.println("-".repeat(78));

        printComparisonRow("Moc A - moi don mot chuyen khu hoi",
                orToolsResult.assignedPointCount(), baselineA, baselineA, -1);
        printComparisonRow("Moc B - dung cach chia xe, sai thu tu",
                orToolsResult.assignedPointCount(), baselineB, baselineA, -1);
        printSolverRow("Nearest Neighbor (giai doan 1)", nearestNeighborResult, matrix);
        printSolverRow("NN + 2-opt tu cai dat", heuristicResult, matrix);
        printSolverRow("OR-Tools (bo giai chinh)", orToolsResult, matrix);

        System.out.println("-".repeat(78));

        /*
         * CANH BAO TRUNG THUC — day la cho de tu lua nhat cua ca bang.
         *
         * Hai bo giai phu khac tap diem thi hai con so km KHONG dat canh nhau duoc: ben
         * bo bot diem di dung nhien tuyen ngan hon, khong phai vi thuat toan tot hon.
         * Voi bo du lieu mau docs/08 thi Nearest Neighbor bo lai dung mot diem, nen neu
         * cu lay 40 km cua no dem so voi 51 km cua OR-Tools thi ket luan se nguoc hoan
         * toan voi su that.
         *
         * Vi vay cot "Giam" cua moi dong tinh theo moc A cua RIENG tap diem dong do, va
         * hai dong duoi chi in ra khi hai ben that su phu cung tap diem.
         */
        boolean sameCoverage =
                nearestNeighborResult.assignedPointCount() == orToolsResult.assignedPointCount()
                        && heuristicResult.assignedPointCount() == orToolsResult.assignedPointCount();

        System.out.printf(Locale.ROOT,
                "So diem duoc gan tren tong %d: NN %d, NN+2-opt %d, OR-Tools %d%n",
                fullPointCount,
                nearestNeighborResult.assignedPointCount(),
                heuristicResult.assignedPointCount(),
                orToolsResult.assignedPointCount());

        if (!sameCoverage) {
            System.out.println();
            System.out.println("CANH BAO: ba bo giai KHONG phu cung tap diem, nen cot Km cua chung");
            System.out.println("khong so sanh truc tiep voi nhau duoc. Cot Giam da tinh rieng theo moc A");
            System.out.println("cua tung tap diem. Muon so cong bang thi chay lai voi doi xe du suc chua");
            System.out.println("cho ca ba ben xep het diem - do chinh la viec cua test");
            System.out.println("bothSolversCoverTheSamePointSet.");
            return;
        }

        System.out.printf(Locale.ROOT,
                "2-opt cai thien them %.1f%% so voi Nearest Neighbor thuan%n",
                MetricsService.reductionPercent(nearestNeighborResult.getOptimizedDistance(),
                        heuristicResult.getOptimizedDistance()));
        System.out.printf(Locale.ROOT,
                "NN + 2-opt con dai hon OR-Tools %.1f%% (day la gioi han cua ban tu cai dat)%n",
                MetricsService.gapPercent(heuristicResult.getOptimizedDistance(),
                        orToolsResult.getOptimizedDistance()));
    }

    /**
     * Mot dong bo giai. Muc giam tinh theo moc A cua CHINH tap diem ma bo giai nay phu,
     * chu khong theo moc A chung — xem comment ve canh bao o ham goi.
     */
    private static void printSolverRow(String label, RoutingResult result, double[][] matrix) {
        double ownBaselineA = MetricsService.baselineA(MetricsService.assignedPoints(result), matrix);
        printComparisonRow(label, result.assignedPointCount(), result.getOptimizedDistance(),
                ownBaselineA, result.getExecutionTimeMs());
    }

    private static void printComparisonRow(String label, int pointCount, double meters,
                                           double baselineAMeters, long executionMs) {
        String reduction = (meters == baselineAMeters)
                ? "-"
                : String.format(Locale.ROOT, "%.1f%%",
                        MetricsService.reductionPercent(baselineAMeters, meters));
        String elapsed = executionMs < 0 ? "-" : executionMs + " ms";
        System.out.printf(Locale.ROOT, "%-38s %5d %7.2f km %8s %9s%n",
                label, pointCount, meters / 1000.0, reduction, elapsed);
    }

    // ==================== Bo du lieu lon de do hieu nang ====================

    private static int readPointCount(String[] args) {
        if (args.length == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(args[0].trim());
        } catch (NumberFormatException notANumber) {
            System.out.println("Tham so phai la so diem giao, vi du: AlgorithmRunner 30");
            return 0;
        }
    }

    /**
     * Sinh n diem giao rai trong noi thanh Ha Noi.
     *
     * DUNG HAT GIONG CO DINH: cung mot n thi luon ra dung mot bo du lieu, chay lai bao
     * nhieu lan cung the. Nho vay con so trong bao cao kiem chung lai duoc — sinh ngau
     * nhien that thi moi lan chay ra mot ket qua khac, chep vao bao cao la khong ai
     * doi chieu duoc.
     *
     * Khoi luong va the tich lay tu ba mat hang that cua docs/08 — co hang nhe ma cong
     * kenh (bong tay trang) lan hang nang ma gon (nuoc lau san) — de bo du lieu thuc su
     * thu ca hai rang buoc suc chua chu khong chi mot.
     */
    private static List<OptimizationPoint> generatedDeliveryPoints(int count) {

        // Khung toa do noi thanh: vi do 20.95-21.06, kinh do 105.76-105.87
        final double latitudeFrom = 20.95;
        final double latitudeTo = 21.06;
        final double longitudeFrom = 105.76;
        final double longitudeTo = 105.87;

        // kg va m3 cua mot thung, chep tu bang san pham docs/03 muc 1
        final double[][] productBoxes = {
                {6.0, 0.085},    // bong tay trang  - nhe, cong kenh
                {5.5, 0.072},    // khan rua mat    - nhe, cong kenh
                {21.5, 0.028},   // nuoc giat 3.5L  - nang, gon
                {24.8, 0.031}    // nuoc lau san 2L - nang, gon
        };

        Random random = new Random(20260824L);

        List<OptimizationPoint> points = new ArrayList<>();
        points.add(OptimizationPoint.depot(DEPOT_LATITUDE, DEPOT_LONGITUDE));

        for (int i = 1; i <= count; i++) {
            double latitude = latitudeFrom + random.nextDouble() * (latitudeTo - latitudeFrom);
            double longitude = longitudeFrom + random.nextDouble() * (longitudeTo - longitudeFrom);

            // Moi don 1-3 mat hang, moi mat hang 2-20 thung
            double weight = 0;
            double volume = 0;
            int lineCount = 1 + random.nextInt(3);
            for (int line = 0; line < lineCount; line++) {
                double[] box = productBoxes[random.nextInt(productBoxes.length)];
                int boxCount = 2 + random.nextInt(19);
                weight += box[0] * boxCount;
                volume += box[1] * boxCount;
            }

            points.add(OptimizationPoint.forOrder((long) i, "Khach " + i,
                    latitude, longitude,
                    Math.round(weight * 10) / 10.0,
                    Math.round(volume * 1000) / 1000.0));
        }
        return points;
    }

    /**
     * Doi xe vua du cho bo du lieu sinh ra, du RONG 25% de ca ba bo giai deu xep het diem.
     *
     * Vi sao phai du rong: thieu suc chua thi Nearest Neighbor bo lai vai diem con OR-Tools
     * xep het, luc do hai cot km khong dat canh nhau duoc nua (xem canh bao o bang so sanh).
     * Muon do RIENG chat luong thuat toan thi phai bo yeu to "ai bo diem" ra khoi phep do.
     */
    private static List<OptimizationVehicle> fleetFor(List<OptimizationPoint> allPoints) {

        double totalWeight = 0;
        double totalVolume = 0;
        for (OptimizationPoint point : allPoints) {
            totalWeight += point.getWeight();
            totalVolume += point.getVolume();
        }

        final double vehicleMaxWeight = 1500;
        final double vehicleMaxVolume = 12.0;

        int vehiclesForWeight = (int) Math.ceil(totalWeight * 1.25 / vehicleMaxWeight);
        int vehiclesForVolume = (int) Math.ceil(totalVolume * 1.25 / vehicleMaxVolume);
        int vehicleCount = Math.max(2, Math.max(vehiclesForWeight, vehiclesForVolume));

        List<OptimizationVehicle> vehicles = new ArrayList<>();
        for (int i = 1; i <= vehicleCount; i++) {
            vehicles.add(new OptimizationVehicle((long) i,
                    String.format("29H-%03d.00", i), vehicleMaxWeight, vehicleMaxVolume));
        }
        return vehicles;
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
