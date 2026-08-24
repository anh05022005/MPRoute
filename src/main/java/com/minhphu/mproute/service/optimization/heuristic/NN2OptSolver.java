package com.minhphu.mproute.service.optimization.heuristic;

import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import com.minhphu.mproute.service.optimization.RoutingAlgorithm;
import com.minhphu.mproute.service.optimization.RoutingResult;

import java.util.List;

/**
 * Ban TU CAI DAT day du: Nearest Neighbor (giai doan 1) roi 2-opt (giai doan 2).
 *
 * Day la thu duoc dem ra so voi OR-Tools o chuong 4 bao cao. Ban than lop nay rat mong
 * — no chi noi hai giai doan lai — nhung viec no cai dat RoutingAlgorithm moi la diem
 * quan trong: nho vay MetricsService goi duoc no va OrToolsSolver bang dung mot cach,
 * tren dung mot ma tran, roi dat hai con so canh nhau.
 *
 * VI SAO TACH LAM HAI GIAI DOAN chu khong viet mot ham lon:
 *   - Giai doan 1 quyet dinh PHAN DON CHO XE NAO (bai toan xep thung, co rang buoc
 *     suc chua). Giai doan 2 quyet dinh THU TU DI TRONG MOT XE (bai toan nguoi giao
 *     hang, khong dung toi suc chua).
 *   - Tach ra thi kiem thu duoc rieng tung cai, va giang lai cung de hon han.
 *   - Va do chinh la HAN CHE cua ban tu cai dat, phai noi thang khi van dap: 2-opt chi
 *     sap lai thu tu BEN TRONG tung tuyen, no khong bao gio chuyen mot diem tu xe nay
 *     sang xe khac. OR-Tools thi lam duoc, do la ly do chinh no thang (docs/07).
 */
public class NN2OptSolver implements RoutingAlgorithm {

    private final NearestNeighborSolver nearestNeighborSolver = new NearestNeighborSolver();
    private final TwoOptImprover twoOptImprover = new TwoOptImprover();

    @Override
    public RoutingResult solve(List<OptimizationPoint> deliveryPoints,
                               List<OptimizationVehicle> vehicles,
                               double[][] matrix) {

        long startedAt = System.currentTimeMillis();

        // GIAI DOAN 1: chia diem cho xe, moi tuyen da thoa man ca hai rang buoc suc chua
        RoutingResult result = nearestNeighborSolver.solve(deliveryPoints, vehicles, matrix);

        /*
         * GIAI DOAN 2: sap lai thu tu trong tung tuyen.
         *
         * Khong dung toi vehicles va khong dung toi unassignedPoints o day: 2-opt chi
         * doi cho cac diem SAN CO trong mot tuyen. Diem giai doan 1 khong xep duoc thi
         * giai doan 2 cung khong cuu duoc — no khong co quyen mo them tuyen.
         */
        twoOptImprover.improveAll(result.getRoutes(), matrix);

        // Thu tu doi thi tong quang duong doi theo, phai tinh lai
        result.setOptimizedDistance(
                RouteDistanceCalculator.totalDistanceOfRoutes(result.getRoutes(), matrix));

        result.setAlgorithm(algorithmType());
        result.setExecutionTimeMs(System.currentTimeMillis() - startedAt);
        return result;
    }

    @Override
    public AlgorithmType algorithmType() {
        return AlgorithmType.NN_2OPT;
    }
}
