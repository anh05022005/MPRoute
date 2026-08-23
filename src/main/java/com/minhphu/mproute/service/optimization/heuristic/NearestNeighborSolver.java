package com.minhphu.mproute.service.optimization.heuristic;

import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import com.minhphu.mproute.service.optimization.RoutingAlgorithm;
import com.minhphu.mproute.service.optimization.RoutingResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Nearest Neighbor — giai doan 1 cua ban TU CAI DAT (docs/03 muc 6).
 *
 * Y TUONG, noi bang mot cau: xe dung o kho, chon diem giao GAN NHAT ma minh con du
 * suc chua. Den noi lai chon diem gan nhat tiep theo. Khong them duoc diem nao nua
 * thi dong tuyen, quay ve kho, chuyen sang xe khac.
 *
 * PACKAGE NAY CAM IMPORT THU VIEN VRP NAO. Day la ban tu cai dat dung lam MOC SO SANH
 * o chuong thuc nghiem — lan thu vien vao la mat y nghia so sanh. Bo giai chinh cua he
 * thong nam o package ben canh, va co hook chan neu hai ben lan vao nhau.
 *
 * Cac vong lap co y viet bang "for" tuong minh chu khong dung Stream: doan nay anh xa
 * 1-1 voi ma gia B1..B28 trong docs/03, va la cho hoi dong chi tay vao hoi tung dong.
 *
 * NHUOC DIEM PHAI NOI DUOC KHI VAN DAP: day la thuat toan THAM LAM — chon tot nhat o
 * moi buoc ma khong nhin xa. Hai hau qua that:
 *   1. Cac diem con lai o xa bi don ve cuoi tuyen, tao chang dai bat hop ly.
 *      Do la ly do can 2-opt o giai doan 2.
 *   2. Viec phan don cho xe von la bai toan XEP THUNG, thuat toan tham lam khong giai
 *      duoc toi uu: co the nhoi xe dau tien gan day roi chua lai mot khoang vo dung,
 *      lam mot don khong con xe nao vua du tong suc chua VAN CON. Xem docs/08.
 *
 * Do phuc tap: O(m x n^2) voi n diem va m xe. n = 100, m = 5 la khoang 50.000 phep
 * so sanh — vai mili giay.
 */
public class NearestNeighborSolver implements RoutingAlgorithm {

    @Override
    public RoutingResult solve(List<OptimizationPoint> deliveryPoints,
                               List<OptimizationVehicle> vehicles,
                               double[][] matrix) {

        long startedAt = System.currentTimeMillis();
        RoutingResult result = new RoutingResult();
        result.setAlgorithm(algorithmType());

        // B1. Ban sao de xoa dan trong khi chay, khong dung vao danh sach goc cua nguoi goi
        List<OptimizationPoint> unassigned = new ArrayList<>(deliveryPoints);

        /*
         * Duyet xe theo TAI TRONG GIAM DAN: xe to nhan nhieu diem truoc thi tong quang
         * duong tot hon. Ly do truc quan: xe to gom duoc ca mot cum khach gan nhau trong
         * mot chuyen, con neu xe be di truoc thi no chi lay duoc vai diem roi xe to phai
         * chay lai chinh vung do lan nua.
         *
         * Sao chep ra danh sach moi truoc khi sap: sap thang vao danh sach cua nguoi goi
         * la lam doi thu tu o cho khac ma ho khong ngo.
         */
        List<OptimizationVehicle> vehiclesByCapacity = new ArrayList<>(vehicles);
        vehiclesByCapacity.sort(Comparator.comparingDouble(OptimizationVehicle::getMaxWeight).reversed());

        // B3. Lan luot tung xe
        for (OptimizationVehicle vehicle : vehiclesByCapacity) {

            // B4. Het diem thi khong can mo them tuyen nua
            if (unassigned.isEmpty()) {
                break;
            }

            OptimizationRoute route = buildRouteFor(vehicle, unassigned, matrix);

            // B27. Xe khong nhan duoc diem nao thi khong tao tuyen rong
            if (!route.isEmpty()) {
                RouteDistanceCalculator.updateTotalDistance(route, matrix);
                result.addRoute(route);
            }
        }

        /*
         * B28. Con diem chua gan sau khi het xe — KHONG duoc im lang bo qua.
         * Day la du lieu de man hinh hien khoi canh bao CN-03-14 va thong bao MSG-08.
         */
        result.setUnassignedPoints(unassigned);

        result.setOptimizedDistance(
                RouteDistanceCalculator.totalDistanceOfRoutes(result.getRoutes(), matrix));
        result.setExecutionTimeMs(System.currentTimeMillis() - startedAt);
        return result;
    }

    /**
     * Chay mot xe cho toi khi no khong nhan them duoc diem nao nua.
     *
     * Ham nay XOA diem khoi danh sach unassigned moi lan nhan mot diem, nen khi tra ve
     * thi unassigned da la phan con lai cho xe sau.
     */
    private OptimizationRoute buildRouteFor(OptimizationVehicle vehicle,
                                            List<OptimizationPoint> unassigned,
                                            double[][] matrix) {

        OptimizationRoute route = new OptimizationRoute(vehicle);

        // B8. Xe bat dau tu kho
        int currentIndex = RouteDistanceCalculator.DEPOT_INDEX;

        // B9. Lap: moi vong nhan them dung mot diem
        while (true) {

            // B10, B11
            OptimizationPoint nearest = null;
            double shortestDistance = Double.MAX_VALUE;

            // B12. Duyet moi diem con lai
            for (OptimizationPoint candidate : unassigned) {

                // B13-B15. Kiem DONG THOI ca hai rang buoc suc chua
                if (!fits(vehicle, route, candidate)) {
                    continue;
                }

                // B17. Tra ma tran da nap san — khong goi OSRM, khong truy van CSDL o day
                double distance = matrix[currentIndex][candidate.getMatrixIndex()];

                // B18-B20
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    nearest = candidate;
                }
            }

            // B21. Khong diem nao vua nua -> dong tuyen. Day la cho thoat vong lap,
            //      thieu no la lap vo han
            if (nearest == null) {
                break;
            }

            // B22-B26
            route.addPoint(nearest);
            unassigned.remove(nearest);
            currentIndex = nearest.getMatrixIndex();
        }

        return route;
    }

    /**
     * Xe con cho duoc diem nay khong — kiem CA HAI rang buoc, khong bao gio chi mot.
     *
     * Vi sao bat buoc ca hai (CBR-03, CBR-04): hang cua Minh Phu co loai nhe ma cong
     * kenh va loai nang ma gon. Xe cho 50 thung bong tay trang moi 300 kg (con xa tai
     * trong 1500 kg) nhung da chiem 4.25 m3. Chi kiem kg thi thuat toan cu nhoi tiep
     * cho toi khi tuyen sinh ra khong xep vua len xe that.
     */
    private boolean fits(OptimizationVehicle vehicle, OptimizationRoute route,
                         OptimizationPoint candidate) {

        if (route.getTotalWeight() + candidate.getWeight() > vehicle.getMaxWeight()) {
            return false;   // qua tai trong
        }
        if (route.getTotalVolume() + candidate.getVolume() > vehicle.getMaxVolume()) {
            return false;   // qua the tich thung xe
        }
        return true;
    }

    @Override
    public AlgorithmType algorithmType() {
        return AlgorithmType.NN;
    }
}
