package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.service.optimization.ortools.OrToolsSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chay benchmark tren bo du lieu chuan Augerat 1995 (CN-09-02).
 *
 * DAY LA SO LIEU AN DIEM CUA CHUONG 4 BAO CAO. Ket qua tren don hang cua Minh Phu rat
 * thuyet phuc voi doanh nghiep nhung khong ai kiem chung duoc; con o day thi loi giai
 * toi uu da duoc cong bo va chung minh, nen con so gap la khach quan.
 *
 * CACH CHAY — giong AlgorithmRunner, bam Run thang trong IDE la de nhat:
 *
 *     mvnw.cmd compile
 *     mvnw.cmd dependency:build-classpath -Dmdep.outputFile=cp.txt
 *     java -cp "target/classes;<noi dung cp.txt>" com...BenchmarkRunner
 *     java -cp "..."                              com...BenchmarkRunner 10   (gioi han 10 giay/bai)
 *
 * KHONG dung "mvnw.cmd exec:java" — classloader rieng cua exec-maven-plugin khong tim
 * thay thu vien native cua OR-Tools. Chi tiet o dau file AlgorithmRunner.
 *
 * Khong can web, khong can MySQL, khong can OSRM: du lieu nam trong resources va
 * khoang cach tinh bang cong thuc Euclid.
 *
 * =========================================================================
 * BA DIEU PHAI GIAI THICH DUOC KHI VAN DAP
 *
 *   1. Vi sao dung DUNG so xe cua de bai — xem VrpInstance.fleet()
 *   2. Vi sao khoang cach phai lam tron ve so nguyen — xem VrpFileReader
 *   3. Doc con so gap the nao cho dung — xem comment o ham printGapTable() ben duoi
 * =========================================================================
 */
public final class BenchmarkRunner {

    /**
     * Muoi bai chon theo docs/07 muc "Bo du lieu dung", trai deu tu 31 den 79 diem.
     *
     * Ba bo A, B, P khac nhau o cach rai diem: A rai ngau nhien, B co cum ro rang, P la
     * ban rut gon cua cac bai truoc do. Chon ca ba de con so gap khong phu thuoc vao mot
     * kieu du lieu duy nhat — chi chay bo A thi hoi dong hoi "the bo khac thi sao" la bi.
     */
    private static final String[] INSTANCE_NAMES = {
            "A-n32-k5", "A-n39-k5", "A-n45-k6", "A-n60-k9", "A-n80-k10",
            "B-n35-k5", "B-n50-k7",
            "P-n40-k5", "P-n55-k7", "P-n76-k5"
    };

    /** Gioi han thoi gian mac dinh cho OR-Tools o moi bai, giay. */
    private static final long DEFAULT_TIME_LIMIT_SECONDS = 5;

    private BenchmarkRunner() {
    }

    /** Ket qua cua OR-Tools tren mot bai, dat canh loi giai toi uu da cong bo. */
    private record InstanceResult(VrpInstance instance, double distance, int routeCount,
                                  int unassignedCount, long elapsedMs) {

        boolean coveredEveryPoint() {
            return unassignedCount == 0;
        }
    }

    public static void main(String[] args) {

        long timeLimitSeconds = readTimeLimit(args);

        System.out.println("=".repeat(90));
        System.out.println("BENCHMARK AUGERAT 1995 - bo du lieu chuan CVRPLIB (CN-09-02)");
        System.out.println("=".repeat(90));
        System.out.printf(Locale.ROOT,
                "Khoang cach EUC_2D lam tron ve so nguyen (quy uoc CVRPLIB). "
                        + "Gioi han OR-Tools %d giay moi bai.%n",
                timeLimitSeconds);
        System.out.println("The tich cua moi diem dat bang 0 nen rang buoc thu hai luon thoa man - "
                + "bo chuan chi co mot chieu.");
        System.out.println();

        List<InstanceResult> results = new ArrayList<>();
        for (String instanceName : INSTANCE_NAMES) {
            results.add(runInstance(VrpFileReader.readInstance(instanceName), timeLimitSeconds));
            System.out.printf(Locale.ROOT, "  da chay xong %s%n", instanceName);
        }
        System.out.println();

        printGapTable(results);
    }

    /**
     * Chay OR-Tools tren mot bai, voi DUNG doi xe cua de bai.
     *
     * deliveryPoints() va fleet() tra ban sao moi moi lan goi, nen bo giai ghi thu tu diem
     * vao doi tuong tuyen cua no ma khong dong vao du lieu goc cua bai.
     */
    private static InstanceResult runInstance(VrpInstance instance, long timeLimitSeconds) {
        RoutingResult result = new OrToolsSolver(timeLimitSeconds)
                .solve(instance.deliveryPoints(), instance.fleet(), instance.getMatrix());

        return new InstanceResult(instance,
                result.getOptimizedDistance(),
                result.getRoutes().size(),
                result.getUnassignedPoints().size(),
                result.getExecutionTimeMs());
    }

    // ==================== Bang gap so voi loi giai toi uu ====================

    /**
     * Bang chinh cua chuong 4 (CN-09-02).
     *
     *     gap (%) = (ket qua cua minh - loi giai toi uu) / loi giai toi uu x 100
     *
     * DOC CON SO NAY THE NAO — chuan bi san cho van dap:
     *
     *  - OR-Tools voi Guided Local Search trong vai giay thuong chi lech vai phan tram.
     *    Khong ra dung 0% la binh thuong: bo giai dung theo gioi han thoi gian chu khong
     *    chung minh toi uu.
     *  - GAP AM MOI LA DAU HIEU SAI: ngan hon loi giai toi uu da chung minh thi gan nhu
     *    chac chan dang tinh thieu, hay gap nhat la quen chang cuoi ve kho (CBR-01). Gap
     *    lon bat thuong thi kiem lai quy uoc lam tron EUC_2D o VrpFileReader.
     *
     * Bai nao khong xep het diem thi danh dau * va KHONG tinh vao trung binh: bo bot diem
     * di duong nhien tuyen ngan hon, dem so canh loi giai toi uu la ket luan nguoc.
     */
    private static void printGapTable(List<InstanceResult> results) {

        System.out.println("=".repeat(90));
        System.out.println("GAP CUA OR-TOOLS SO VOI LOI GIAI TOI UU DA CONG BO (CN-09-02)");
        System.out.println("=".repeat(90));
        System.out.printf(Locale.ROOT, "%-11s %5s %4s %8s | %9s %8s %10s%n",
                "Bai toan", "Diem", "Xe", "Toi uu", "OR-Tools", "Gap", "Thoi gian");
        System.out.println("-".repeat(90));

        double sumGap = 0;
        int counted = 0;

        for (InstanceResult result : results) {
            VrpInstance instance = result.instance();
            double optimal = instance.getOptimalValue();

            System.out.printf(Locale.ROOT, "%-11s %5d %4d %8.0f | %9s %8s %7d ms%n",
                    instance.getName(), instance.deliveryPointCount(), instance.getVehicleCount(),
                    optimal, distanceCell(result), gapCell(result, optimal), result.elapsedMs());

            if (result.coveredEveryPoint()) {
                sumGap += MetricsService.gapPercent(result.distance(), optimal);
                counted++;
            }
        }

        System.out.println("-".repeat(90));
        System.out.printf(Locale.ROOT, "%-11s %5s %4s %8s | %9s %8s%n",
                "Trung binh", "", "", "", "", averageCell(sumGap, counted));
        System.out.println();
        System.out.printf(Locale.ROOT, "So bai tinh vao trung binh: %d/%d%n", counted, results.size());
        System.out.println("Dau * la bai khong xep het diem vao doi xe cua de bai, quang duong cua");
        System.out.println("dong do khong dat canh loi giai toi uu duoc nen khong tinh vao trung binh.");
        System.out.println();
    }

    // ==================== Ham in ====================

    /** Quang duong, kem dau * neu bo giai de sot diem. */
    private static String distanceCell(InstanceResult result) {
        return String.format(Locale.ROOT, "%.0f%s",
                result.distance(), result.coveredEveryPoint() ? "" : "*");
    }

    private static String gapCell(InstanceResult result, double optimalValue) {
        if (!result.coveredEveryPoint()) {
            return "sot " + result.unassignedCount();
        }
        return String.format(Locale.ROOT, "%+.1f%%", MetricsService.gapPercent(result.distance(), optimalValue));
    }

    private static String averageCell(double sum, int count) {
        return count == 0 ? "-" : String.format(Locale.ROOT, "%.1f%%", sum / count);
    }

    private static long readTimeLimit(String[] args) {
        if (args.length == 0) {
            return DEFAULT_TIME_LIMIT_SECONDS;
        }
        try {
            return Long.parseLong(args[0].trim());
        } catch (NumberFormatException notANumber) {
            System.out.println("Tham so phai la so giay gioi han cho OR-Tools, vi du: BenchmarkRunner 10");
            return DEFAULT_TIME_LIMIT_SECONDS;
        }
    }
}
