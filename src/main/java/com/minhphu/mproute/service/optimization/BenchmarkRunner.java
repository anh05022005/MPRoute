package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.service.optimization.heuristic.NN2OptSolver;
import com.minhphu.mproute.service.optimization.heuristic.NearestNeighborSolver;
import com.minhphu.mproute.service.optimization.ortools.OrToolsSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chay benchmark tren bo du lieu chuan Augerat 1995 (CN-09-02, CN-09-03).
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
 *   3. Vi sao gap cua NN+2-opt van con hai chuc phan tram, va vi sao do KHONG
 *      phai dau hieu code sai — xem comment o ham printGapTable() ben duoi
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

    /** Ket qua cua mot bo giai tren mot bai. */
    private record SolverRun(String solverName, double distance, int routeCount,
                             int unassignedCount, long elapsedMs) {

        boolean coveredEveryPoint() {
            return unassignedCount == 0;
        }
    }

    /** Ba dong ket qua cua mot bai, dat canh loi giai toi uu da cong bo. */
    private record InstanceResult(VrpInstance instance, SolverRun nearestNeighbor,
                                  SolverRun twoOpt, SolverRun orTools) {
    }

    public static void main(String[] args) {

        long timeLimitSeconds = readTimeLimit(args);

        System.out.println("=".repeat(112));
        System.out.println("BENCHMARK AUGERAT 1995 - bo du lieu chuan CVRPLIB (CN-09-02)");
        System.out.println("=".repeat(112));
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
        printImprovementTable(results);
    }

    /** Chay ca ba bo giai tren CUNG mot ma tran va CUNG doi xe cua bai do. */
    private static InstanceResult runInstance(VrpInstance instance, long timeLimitSeconds) {

        double[][] matrix = instance.getMatrix();

        SolverRun nearestNeighbor = run("NN", new NearestNeighborSolver(), instance, matrix);
        SolverRun twoOpt = run("NN+2-opt", new NN2OptSolver(), instance, matrix);
        SolverRun orTools = run("OR-Tools", new OrToolsSolver(timeLimitSeconds), instance, matrix);

        return new InstanceResult(instance, nearestNeighbor, twoOpt, orTools);
    }

    /**
     * Mot lan chay.
     *
     * Moi bo giai nhan mot DANH SACH DIEM MOI va mot DOI XE MOI: NearestNeighborSolver
     * xoa dan phan tu khoi danh sach no dang chay, con cac bo giai deu ghi thu tu diem
     * vao doi tuong tuyen cua rieng minh. Dung chung mot danh sach thi bo chay sau nhan
     * dau vao da bi bo chay truoc dong vao.
     */
    private static SolverRun run(String solverName, RoutingAlgorithm solver,
                                 VrpInstance instance, double[][] matrix) {

        RoutingResult result = solver.solve(instance.deliveryPoints(), instance.fleet(), matrix);

        return new SolverRun(solverName,
                result.getOptimizedDistance(),
                result.getRoutes().size(),
                result.getUnassignedPoints().size(),
                result.getExecutionTimeMs());
    }

    // ==================== Bang 1: gap so voi loi giai toi uu ====================

    /**
     * Bang chinh cua chuong 4 (CN-09-02).
     *
     *     gap (%) = (ket qua cua minh - loi giai toi uu) / loi giai toi uu x 100
     *
     * DOC CON SO NAY THE NAO — chuan bi san cho vấn đáp:
     *
     *  - NN thuan thuong lech 20-35%, them 2-opt keo xuong khoang 10-25%. Nam trong
     *    khoang do la binh thuong, KHONG phai dau hieu code sai.
     *  - Vi sao 2-opt khong keo xuong duoc nua: no chi sap lai THU TU BEN TRONG mot
     *    tuyen, khong dong duoc vao viec diem nao thuoc xe nao. Ma voi CVRP thi phan
     *    sai so lon nhat lai nam chinh o khau phan diem cho xe. Muon giam tiep phai
     *    them buoc hoan doi diem GIUA cac tuyen (Or-opt, relocate) — do la huong phat
     *    trien, khong phai pham vi do an.
     *  - GAP QUA THAP MOI LA DAU HIEU SAI: duoi 5% thi gan nhu chac chan dang tinh
     *    thieu, hay gap nhat la quen chang cuoi ve kho (CBR-01). Tren 50% thi kiem lai
     *    quy uoc lam tron EUC_2D o VrpFileReader.
     *
     * Bai nao co bo giai khong xep het diem thi danh dau * va KHONG tinh vao trung binh:
     * ben bo bot diem di duong nhien tuyen ngan hon, dem so canh nhau la ket luan nguoc.
     */
    private static void printGapTable(List<InstanceResult> results) {

        System.out.println("=".repeat(112));
        System.out.println("BANG 1 - GAP SO VOI LOI GIAI TOI UU DA CONG BO (CN-09-02)");
        System.out.println("=".repeat(112));
        System.out.printf(Locale.ROOT, "%-11s %5s %4s %8s | %9s %8s | %9s %8s | %9s %8s%n",
                "Bai toan", "Diem", "Xe", "Toi uu",
                "NN", "Gap", "NN+2-opt", "Gap", "OR-Tools", "Gap");
        System.out.println("-".repeat(112));

        double sumGapNearestNeighbor = 0;
        double sumGapTwoOpt = 0;
        double sumGapOrTools = 0;
        int countedNearestNeighbor = 0;
        int countedTwoOpt = 0;
        int countedOrTools = 0;

        for (InstanceResult result : results) {
            VrpInstance instance = result.instance();
            double optimal = instance.getOptimalValue();

            System.out.printf(Locale.ROOT, "%-11s %5d %4d %8.0f | %9s %8s | %9s %8s | %9s %8s%n",
                    instance.getName(), instance.deliveryPointCount(), instance.getVehicleCount(),
                    optimal,
                    distanceCell(result.nearestNeighbor()), gapCell(result.nearestNeighbor(), optimal),
                    distanceCell(result.twoOpt()), gapCell(result.twoOpt(), optimal),
                    distanceCell(result.orTools()), gapCell(result.orTools(), optimal));

            if (result.nearestNeighbor().coveredEveryPoint()) {
                sumGapNearestNeighbor += MetricsService.gapPercent(result.nearestNeighbor().distance(), optimal);
                countedNearestNeighbor++;
            }
            if (result.twoOpt().coveredEveryPoint()) {
                sumGapTwoOpt += MetricsService.gapPercent(result.twoOpt().distance(), optimal);
                countedTwoOpt++;
            }
            if (result.orTools().coveredEveryPoint()) {
                sumGapOrTools += MetricsService.gapPercent(result.orTools().distance(), optimal);
                countedOrTools++;
            }
        }

        System.out.println("-".repeat(112));
        System.out.printf(Locale.ROOT, "%-11s %5s %4s %8s | %9s %8s | %9s %8s | %9s %8s%n",
                "Trung binh", "", "", "",
                "", averageCell(sumGapNearestNeighbor, countedNearestNeighbor),
                "", averageCell(sumGapTwoOpt, countedTwoOpt),
                "", averageCell(sumGapOrTools, countedOrTools));
        System.out.println();
        System.out.printf(Locale.ROOT,
                "So bai tinh vao trung binh: NN %d/%d, NN+2-opt %d/%d, OR-Tools %d/%d%n",
                countedNearestNeighbor, results.size(),
                countedTwoOpt, results.size(),
                countedOrTools, results.size());
        System.out.println("Dau * la bo giai khong xep het diem vao doi xe cua de bai, quang duong cua");
        System.out.println("dong do khong dat canh loi giai toi uu duoc nen khong tinh vao trung binh.");
        System.out.println();
    }

    // ==================== Bang 2: NN so voi NN + 2-opt ====================

    /**
     * Bang so sanh hai bo giai TU CAI DAT (CN-09-03).
     *
     * Bang 1 do minh cach loi giai tot nhat bao nhieu; bang nay do RIENG dong gop cua
     * buoc 2-opt, tuc phan cai thien ma chinh minh viet ra. Hai bang tra loi hai cau hoi
     * khac nhau nen phai in ca hai.
     *
     * Chi in dong nao ma CA HAI ben deu xep het diem: mot ben bo diem thi hai con so
     * khong con do cung mot thu.
     */
    private static void printImprovementTable(List<InstanceResult> results) {

        System.out.println("=".repeat(112));
        System.out.println("BANG 2 - RIENG DONG GOP CUA BUOC 2-OPT (CN-09-03)");
        System.out.println("=".repeat(112));
        System.out.printf(Locale.ROOT, "%-11s %5s %10s %10s %11s %10s %10s%n",
                "Bai toan", "Diem", "NN", "NN+2-opt", "Cai thien", "t(NN)", "t(2-opt)");
        System.out.println("-".repeat(112));

        double sumImprovement = 0;
        int counted = 0;

        for (InstanceResult result : results) {
            SolverRun nearestNeighbor = result.nearestNeighbor();
            SolverRun twoOpt = result.twoOpt();

            boolean comparable = nearestNeighbor.coveredEveryPoint() && twoOpt.coveredEveryPoint();
            String improvement = "-";
            if (comparable) {
                double percent = MetricsService.reductionPercent(
                        nearestNeighbor.distance(), twoOpt.distance());
                improvement = String.format(Locale.ROOT, "%.1f%%", percent);
                sumImprovement += percent;
                counted++;
            }

            System.out.printf(Locale.ROOT, "%-11s %5d %10s %10s %11s %8d ms %8d ms%n",
                    result.instance().getName(), result.instance().deliveryPointCount(),
                    distanceCell(nearestNeighbor), distanceCell(twoOpt), improvement,
                    nearestNeighbor.elapsedMs(), twoOpt.elapsedMs());
        }

        System.out.println("-".repeat(112));
        System.out.printf(Locale.ROOT, "%-11s %5s %10s %10s %11s%n",
                "Trung binh", "", "", "", averageCell(sumImprovement, counted));
        System.out.println();
        System.out.println("Cai thien = (NN - NN2opt) / NN x 100. Duong nghia la 2-opt lam tuyen ngan di.");
        System.out.println("2-opt KHONG BAO GIO duoc lam tuyen dai them - do la test");
        System.out.println("twoOptNeverIncreasesTotalDistance kiem, va la tinh chat cua chinh thuat toan.");
    }

    // ==================== Ham in ====================

    /** Quang duong, kem dau * neu bo giai do de sot diem. */
    private static String distanceCell(SolverRun run) {
        return String.format(Locale.ROOT, "%.0f%s",
                run.distance(), run.coveredEveryPoint() ? "" : "*");
    }

    private static String gapCell(SolverRun run, double optimalValue) {
        if (!run.coveredEveryPoint()) {
            return "sot " + run.unassignedCount();
        }
        return String.format(Locale.ROOT, "%+.1f%%", MetricsService.gapPercent(run.distance(), optimalValue));
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
