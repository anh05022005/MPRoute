package com.minhphu.mproute.service.optimization;

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
    double khoangCach(OptimizationPoint diemA, OptimizationPoint diemB);

    /** Ten nguon du lieu, de ghi vao bang ket qua va hien ra man hinh. */
    String sourceName();
}
