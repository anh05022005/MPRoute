package com.minhphu.mproute.service.optimization;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Mot tuyen giao hang: mot xe, mot danh sach diem giao theo dung thu tu se di.
 *
 * CHU Y — day la cho hay hieu nham nhat cua ca do an:
 * danh sach "points" KHONG chua kho. Kho la ngam dinh o dau va o cuoi tuyen.
 * Tuyen ghi la [A, B, C] nghia la xe di KHO -> A -> B -> C -> KHO.
 *
 * Cach nay lam viec kiem rang buoc gon han (chi cong nhu cau cua cac diem that),
 * nhung doi lai HAM TINH TONG QUANG DUONG PHAI TU CONG hai chang cua kho —
 * xem RouteDistanceCalculator. Quen chang cuoi ve kho la loi kinh dien nhat cua
 * do an VRP (CBR-01).
 */
@Getter
@Setter
public class OptimizationRoute {

    private OptimizationVehicle vehicle;

    /** Chi cac diem giao, KHONG co kho. Thu tu trong danh sach la thu tu di. */
    private List<OptimizationPoint> points = new ArrayList<>();

    /** Don vi met, DA gom ca chang kho -> diem dau va chang diem cuoi -> kho. */
    private double totalDistance;

    // Tong nhu cau cua cac diem trong tuyen, de doi chieu voi suc chua cua xe
    private double totalWeight;
    private double totalVolume;

    public OptimizationRoute(OptimizationVehicle vehicle) {
        this.vehicle = vehicle;
    }

    /**
     * Them mot diem vao CUOI tuyen va cong don nhu cau.
     *
     * Cong don ngay tai day chu khong tinh lai bang cach duyet ca danh sach moi lan:
     * Nearest Neighbor goi ham nay trong vong lap, tinh lai moi lan la thua.
     */
    public void addPoint(OptimizationPoint point) {
        points.add(point);
        totalWeight += point.getWeight();
        totalVolume += point.getVolume();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    /** Ty le lap day theo khoi luong, de hien tren man hinh ket qua (CN-03-07). */
    public double weightUtilization() {
        return (vehicle == null || vehicle.getMaxWeight() == 0) ? 0 : totalWeight / vehicle.getMaxWeight();
    }

    public double volumeUtilization() {
        return (vehicle == null || vehicle.getMaxVolume() == 0) ? 0 : totalVolume / vehicle.getMaxVolume();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append(vehicle == null ? "(chua co xe)" : vehicle.getLicensePlate());
        text.append("  ").append(points.size()).append(" diem: KHO");
        for (OptimizationPoint point : points) {
            text.append(" -> ").append(point.getName());
        }
        text.append(" -> KHO");
        return text.toString();
    }
}
