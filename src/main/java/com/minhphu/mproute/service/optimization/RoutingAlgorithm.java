package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.entity.enums.AlgorithmType;

import java.util.List;

/**
 * Mot cach phan tuyen. Do an co hai cai dat:
 *
 *   ortools/OrToolsSolver     — bo giai CHINH, dung thu vien Google OR-Tools
 *   heuristic/NN2OptSolver    — ban TU CAI DAT, giu lam moc so sanh o chuong thuc nghiem
 *
 * Day la Strategy pattern thu hai cua do an (cai thu nhat la DistanceCalculator).
 * Ca hai nhan cung dau vao va tra cung kieu ket qua, nen chay lan luot ca hai tren
 * CUNG MOT MA TRAN roi dat hai con so canh nhau la xong bang so sanh chuong 4 —
 * khong phai sua mot dong nao cua man hinh phan tuyen de doi bo giai (CN-03-04).
 *
 * LUU Y VE HIEU NANG: tham so matrix la ma tran DA NAP SAN vao bo nho. Cai dat cua
 * interface nay khong duoc goi OSRM hay truy van CSDL ben trong vong lap — 2-opt hoi
 * khoang cach hang nghin lan cho mot tuyen.
 */
public interface RoutingAlgorithm {

    /**
     * Phan cac diem giao vao cac xe va sap thu tu di trong tung xe.
     *
     * @param deliveryPoints chi cac diem GIAO, khong gom kho. Kho la chi so 0 cua ma tran
     * @param vehicles       cac xe duoc phep dung cho lan chay nay
     * @param matrix         ma tran khoang cach da doi xung hoa, met, chi so 0 la kho
     * @return ket qua gom cac tuyen VA danh sach diem khong xep duoc (khong bao gio null)
     */
    RoutingResult solve(List<OptimizationPoint> deliveryPoints,
                        List<OptimizationVehicle> vehicles,
                        double[][] matrix);

    /** De ghi vao bang ket qua va hien ra o chon thuat toan tren man hinh. */
    AlgorithmType algorithmType();
}
