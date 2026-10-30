package com.minhphu.mproute.service.optimization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tinh cac con so cua bang so sanh chuong 4 bao cao (docs/03 muc 11, docs/07).
 *
 * DAY LA LOP BIEN THUAT TOAN THANH KET QUA DO AN. Tuyen ngan hon bao nhieu phan tram
 * so voi cach Minh Phu dang lam — do la cau tra loi cho cau hoi "de tai nay giai quyet
 * duoc gi", va no phai co so kem theo chu khong noi chung chung duoc.
 *
 * BON MOC, moi moc tra loi mot cau hoi khac nhau:
 *
 *   Moc A — moi don mot chuyen khu hoi rieng tu kho.
 *           Tra loi: "so voi cach hien tai thi giam bao nhieu".
 *           Con so dep (40-50%) nhung DE BI BE: hoi dong se hoi "co that Minh Phu di
 *           kieu do khong, hay em tu dung ra mot cach lam kem roi so voi no".
 *
 *   Moc B — dung DUNG cach chia xe ma thuat toan da tao ra, nhung giao theo thu tu
 *           NHAN DON (orderId tang dan) thay vi thu tu toi uu.
 *           Tra loi: "rieng viec sap thu tu dong gop bao nhieu".
 *           Con so khiem ton (8-15%) nhung DANG TIN HON HAN, vi hai ben chi khac nhau
 *           dung mot thu la thu tu di.
 *
 *   NN + 2-opt — ban tu cai dat. Tra loi: "em tu lam duoc den dau".
 *
 *   Moc C — OR-Tools, bo giai chinh. Tra loi: "phan tu lam con kem loi giai tot bao nhieu".
 *
 * BAO CAO CA BON. Mot con so 45% khong kem boi canh yeu hon han bon con so co giai thich.
 * Biet gioi han cua giai phap minh viet ra la mot ket qua, khong phai mot thieu sot.
 *
 * Lop nay khong giu trang thai, khong dung CSDL, khong dung mang — de AlgorithmRunner
 * chay bang main() va test JUnit deu goi thang duoc.
 */
public final class MetricsService {

    private MetricsService() {
        // lop tien ich, khong tao doi tuong
    }

    /**
     * MOC A — mo phong cach lam hien tai: moi don mot chuyen khu hoi rieng tu kho.
     *
     *     baselineA = TONG (2 x matrix[KHO][diem]) voi moi diem giao
     *
     * Nhan 2 vi mot chuyen gom ca luot di lan luot ve. Quen so 2 nay la moc A be di mot
     * nua, muc giam trong khiem ton han thuc te.
     */
    public static double baselineA(List<OptimizationPoint> deliveryPoints, double[][] matrix) {
        double total = 0;
        for (OptimizationPoint point : deliveryPoints) {
            total += 2 * matrix[RouteDistanceCalculator.DEPOT_INDEX][point.getMatrixIndex()];
        }
        return total;
    }

    /**
     * MOC B — giu nguyen cach chia xe, chi doi thu tu giao ve thu tu nhan don.
     *
     * Muc dich la CO LAP dong gop cua viec sap thu tu: hai ben co cung so tuyen, cung
     * cach phan don cho xe, cung tap diem — khac nhau dung mot thu la thu tu di. Chenh
     * lech do vay quy hoan toan ve phan sap thu tu (Nearest Neighbor + 2-opt).
     *
     * Sap theo orderId tang dan chinh la thu tu don ve trong ngay, tuc thu tu ma nguoi
     * dieu phoi se giao neu khong co he thong nao ho tro.
     *
     * KHONG DONG VAO TUYEN THAT: ham tao tuyen tam de tinh, vi day chi la con so bao cao.
     * Sap thang vao route.getPoints() la pha ket qua phan tuyen that su.
     */
    public static double baselineB(List<OptimizationRoute> routes, double[][] matrix) {
        double total = 0;
        for (OptimizationRoute route : routes) {

            OptimizationRoute routeInOrderReceived = new OptimizationRoute(route.getVehicle());
            List<OptimizationPoint> pointsByOrderId = new ArrayList<>(route.getPoints());

            /*
             * Du lieu benchmark Augerat khong co orderId (khong phai don hang that), luc
             * do lay matrixIndex thay the — no cung chinh la thu tu diem trong file .vrp.
             */
            pointsByOrderId.sort(Comparator.comparingLong(point ->
                    point.getOrderId() == null ? point.getMatrixIndex() : point.getOrderId()));

            for (OptimizationPoint point : pointsByOrderId) {
                routeInOrderReceived.addPoint(point);
            }

            total += RouteDistanceCalculator.calculateTotalDistance(routeInOrderReceived, matrix);
        }
        return total;
    }

    /**
     * Tinh va ghi ca hai moc vao ket qua, de man hinh phan tuyen do thang ra bang chi so
     * (CN-03-08).
     *
     * MOC A TINH TREN CAC DIEM DA DUOC XEP TUYEN, khong tinh tren toan bo diem dau vao.
     * Ly do: thieu suc chua thi vai diem bi bo lai, ma so sanh chi co nghia khi hai ben
     * phu DUNG MOT TAP DIEM. Cong ca diem bi bo vao moc A la lam moc A phong len va muc
     * giam trong dep gia tao.
     */
    public static void fillBaselines(RoutingResult result, double[][] matrix) {
        result.setBaselineADistance(baselineA(assignedPoints(result), matrix));
        result.setBaselineBDistance(baselineB(result.getRoutes(), matrix));
    }

    /**
     * Cac diem THUC SU da duoc xep vao tuyen cua mot ket qua.
     *
     * Dung de doi chieu hai bo giai co phu cung tap diem khong: chi khi CUNG TAP DIEM
     * thi hai con so km moi dat canh nhau duoc. Ben nao bo bot diem thi tuyen ngan hon
     * la duong nhien, khong phai vi thuat toan tot hon.
     */
    public static List<OptimizationPoint> assignedPoints(RoutingResult result) {
        List<OptimizationPoint> points = new ArrayList<>();
        for (OptimizationRoute route : result.getRoutes()) {
            points.addAll(route.getPoints());
        }
        return points;
    }

    /**
     * Muc giam so voi mot moc, tinh bang phan tram.
     *
     *     reduction = (moc - toi uu) / moc x 100
     *
     * Duong nghia la tuyen toi uu NGAN HON moc. Moc bang 0 (khong co diem nao) tra ve 0
     * chu khong chia cho 0.
     */
    public static double reductionPercent(double baselineDistance, double optimizedDistance) {
        if (baselineDistance <= 0) {
            return 0;
        }
        return (baselineDistance - optimizedDistance) / baselineDistance * 100.0;
    }

    /**
     * Khoang cach toi loi giai tot hon, tinh bang phan tram — chinh la "gap" cua chuong 4.
     *
     *     gap = (ket qua cua minh - ket qua tham chieu) / ket qua tham chieu x 100
     *
     * Dung cho hai viec: do NN+2-opt cach OR-Tools bao nhieu, va do ca hai cach loi giai
     * toi uu da biet cua bo benchmark Augerat bao nhieu (tuan 10).
     */
    public static double gapPercent(double candidateDistance, double referenceDistance) {
        if (referenceDistance <= 0) {
            return 0;
        }
        return (candidateDistance - referenceDistance) / referenceDistance * 100.0;
    }

    /** Ty le lap day trung binh theo khoi luong cua cac tuyen, 0..1 (CN-03-07). */
    public static double averageWeightUtilization(List<OptimizationRoute> routes) {
        if (routes.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (OptimizationRoute route : routes) {
            total += route.weightUtilization();
        }
        return total / routes.size();
    }

    /** Ty le lap day trung binh theo the tich. Luon bao cao CUNG voi ty le khoi luong. */
    public static double averageVolumeUtilization(List<OptimizationRoute> routes) {
        if (routes.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (OptimizationRoute route : routes) {
            total += route.volumeUtilization();
        }
        return total / routes.size();
    }
}
