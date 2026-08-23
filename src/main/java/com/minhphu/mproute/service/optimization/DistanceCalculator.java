package com.minhphu.mproute.service.optimization;

import java.util.List;

/**
 * Cach tinh khoang cach giua hai diem. Do an co hai cai dat:
 *
 *   OsrmDistanceCalculator       — khoang cach duong di that, goi may chu OSRM cuc bo
 *   HaversineDistanceCalculator  — duong chim bay nhan he so quy doi, khong can mang
 *
 * Day la Strategy pattern. Doi cai dat khong phai sua mot dong nao cua thuat toan,
 * va do chinh la thu cho phep he thong van chay khi OSRM chet (CBR-11).
 *
 * LUU Y VE HIEU NANG: khong duoc goi ham nay ben trong vong lap cua thuat toan.
 * 2-opt hoi khoang cach hang nghin lan, moi lan mot HTTP request la treo chuong trinh.
 * Cach dung dung: DistanceMatrixService nap san toan bo ma tran vao bo nho mot lan,
 * thuat toan chi tra matrix[i][j].
 */
public interface DistanceCalculator {

    /** Khoang cach giua hai diem, don vi MET. */
    double distanceBetween(OptimizationPoint from, OptimizationPoint to);

    /** Ten nguon du lieu, de ghi vao bang ket qua va hien ra man hinh. */
    String sourceName();

    /**
     * Dung ca ma tran tu mot bo tinh, dong thoi danh so matrixIndex cho tung diem.
     *
     * Dung cho AlgorithmRunner chay bang main() va cho unit test — nhung cho khong co
     * CSDL de tra bo nho dem. Duong CHAY THAT cua he thong la DistanceMatrixService:
     * o do co bo nho dem, co goi OSRM mot lan cho ca ma tran, va co buoc doi xung hoa.
     *
     * Diem dau tien trong danh sach phai la kho (quy uoc chi so 0).
     */
    static double[][] buildMatrix(List<OptimizationPoint> points, DistanceCalculator calculator) {
        int size = points.size();
        double[][] matrix = new double[size][size];

        for (int i = 0; i < size; i++) {
            points.get(i).setMatrixIndex(i);
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                // Duong cheo bang 0: di tu mot diem toi chinh no
                matrix[i][j] = (i == j) ? 0 : calculator.distanceBetween(points.get(i), points.get(j));
            }
        }
        return matrix;
    }
}
