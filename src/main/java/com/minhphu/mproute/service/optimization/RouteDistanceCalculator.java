package com.minhphu.mproute.service.optimization;

import java.util.List;

/**
 * Tinh tong quang duong cua mot tuyen.
 *
 * DAY LA HAM QUAN TRONG NHAT CUA CA PHAN THUAT TOAN, va cung la cho hay sai nhat.
 * Moi chi so so sanh o chuong 4 — muc giam so voi moc A, moc B, gap benchmark — deu
 * quy ve ham nay. Sai o day thi toan bo bang so lieu sai theo ma van trong hop ly.
 *
 * Dat rieng mot lop, dung static, de CA HAI bo giai (OR-Tools va NN+2-opt tu cai dat)
 * cung goi mot cai. Hai ben tu tinh rieng thi chi can lech mot chang la bang so sanh
 * mat y nghia.
 *
 * Lop nay khong phai @Service: no khong giu trang thai gi, khong dung DB, khong dung
 * mang. Nho vay test JUnit goi thang, va AlgorithmRunner chay bang main() cung goi thang.
 */
public final class RouteDistanceCalculator {

    /** Chi so 0 trong ma tran LUON la kho. Quy uoc xuyen suot do an. */
    public static final int DEPOT_INDEX = 0;

    private RouteDistanceCalculator() {
        // lop tien ich, khong tao doi tuong
    }

    /**
     * Tong quang duong mot tuyen, don vi met.
     *
     * Tuyen [A, B, C] nghia la xe di KHO -> A -> B -> C -> KHO, nen tong gom BA phan:
     *
     *   1. chang dau  : kho -> A
     *   2. cac chang giua : A -> B, B -> C
     *   3. CHANG CUOI : C -> kho          <-- cho hay bi quen nhat (CBR-01)
     *
     * Quen chang cuoi thi moi tuyen bi thieu mot doan, muc giam so voi moc A trong
     * nhu cao bat thuong, va gap benchmark Augerat tut xuong duoi 5% — do chinh la
     * dau hieu de tu phat hien loi nay.
     */
    public static double calculateTotalDistance(OptimizationRoute route, double[][] matrix) {
        List<OptimizationPoint> points = route.getPoints();

        // Tuyen rong: xe khong roi kho nen khong di met nao
        if (points.isEmpty()) {
            return 0;
        }

        // 1. Chang dau: tu kho toi diem giao dau tien
        double total = matrix[DEPOT_INDEX][points.get(0).getMatrixIndex()];

        // 2. Cac chang giua cac diem giao
        for (int i = 0; i < points.size() - 1; i++) {
            int from = points.get(i).getMatrixIndex();
            int to = points.get(i + 1).getMatrixIndex();
            total += matrix[from][to];
        }

        // 3. CHANG CUOI: tu diem giao cuoi cung QUAY VE KHO.
        //    Xe phai ve kho de tra tien, tra hang thua va do xe qua dem.
        int lastPointIndex = points.get(points.size() - 1).getMatrixIndex();
        total += matrix[lastPointIndex][DEPOT_INDEX];

        return total;
    }

    /** Tinh lai va ghi vao tuyen luon, dung sau moi lan thuat toan doi thu tu diem. */
    public static void updateTotalDistance(OptimizationRoute route, double[][] matrix) {
        route.setTotalDistance(calculateTotalDistance(route, matrix));
    }

    /** Tong quang duong cua nhieu tuyen cong lai — con so chinh cua mot lan chay. */
    public static double totalDistanceOfRoutes(List<OptimizationRoute> routes, double[][] matrix) {
        double total = 0;
        for (OptimizationRoute route : routes) {
            total += calculateTotalDistance(route, matrix);
        }
        return total;
    }
}
