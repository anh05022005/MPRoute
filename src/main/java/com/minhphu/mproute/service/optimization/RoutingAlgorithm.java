package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.entity.enums.AlgorithmType;

import java.util.List;

/**
 * Mot cach phan tuyen. Hien chi con mot cai dat: ortools/OrToolsSolver, dung thu vien
 * Google OR-Tools. Hai bo giai tu cai dat NN va NN + 2-opt da bo (docs/09 muc A7).
 *
 * Van giu interface nay de RoutingService va cac test chi phu thuoc vao hop dong
 * "dau vao - ket qua", khong dinh chat vao lop cua thu vien.
 *
 * LUU Y VE HIEU NANG: tham so matrix la ma tran DA NAP SAN vao bo nho. Cai dat cua
 * interface nay khong duoc goi OSRM hay truy van CSDL ben trong vong lap — bo giai hoi
 * khoang cach hang nghin lan cho mot lan chay.
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

    /** De ghi vao bang ket qua cua lan chay. */
    AlgorithmType algorithmType();
}
