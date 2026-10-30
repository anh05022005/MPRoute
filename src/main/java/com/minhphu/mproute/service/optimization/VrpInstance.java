package com.minhphu.mproute.service.optimization;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Mot bai toan CVRP chuan doc tu file .vrp cua bo Augerat 1995 (CN-09-02).
 *
 * DAY LA DU LIEU AN DIEM CUA CHUONG 4. Khac voi don hang cua Minh Phu — vo cung
 * thuyet phuc voi doanh nghiep nhung khong ai kiem chung duoc — bo Augerat da duoc
 * cong dong nghien cuu cong bo kem LOI GIAI TOI UU DA CHUNG MINH. So sanh voi no cho
 * ra mot con so khach quan: thuat toan cua do an cach loi giai tot nhat bao nhieu
 * phan tram.
 *
 * HAI CHO PHAI CAN THAN, sai la moi con so gap deu sai theo:
 *
 * 1. KHOANG CACH EUC_2D LAM TRON VE SO NGUYEN GAN NHAT. Day la quy uoc cua CVRPLIB,
 *    khong phai lam tron cho dep. Loi giai toi uu 784 cua A-n32-k5 duoc tinh bang
 *    dung quy uoc do; giu so thuc thi ket qua lech vai don vi va gap sai theo.
 *
 * 2. BO CHUAN CHI CO MOT RANG BUOC (demand), he thong cua do an co hai (kg va m3).
 *    Cach xu ly: gan volume = 0 cho moi diem, con suc chua the tich cua xe de mot so
 *    duong bat ky. Rang buoc the tich khi do luon thoa man, bai toan tro ve dung CVRP
 *    mot chieu — nhung phan code kiem hai rang buoc van chay nguyen ven chu khong bi
 *    tat di de chay benchmark.
 */
@Getter
public class VrpInstance {

    /**
     * Suc chua the tich cua xe khi chay benchmark.
     *
     * KHONG dat vo cuc du docs/07 goi la "vo cuc": OrToolsSolver quy the tich sang cm3
     * bang cach nhan mot trieu, nhan voi Double.MAX_VALUE la tran kieu long. Moi diem
     * deu co volume = 0 nen bat ky so duong nao cung cho ket qua y het nhau, va mot
     * so huu han thi khong co rui ro tran.
     */
    public static final double UNCONSTRAINED_VOLUME = 1.0;

    private final String name;

    /** So diem KE CA kho — dung nghia DIMENSION cua file .vrp. */
    private final int dimension;

    /** Suc chua cua moi xe, don vi cua bo du lieu (khong phai kg). */
    private final double capacity;

    /** So xe cua bai toan, doc tu "No of trucks" trong dong COMMENT. */
    private final int vehicleCount;

    /** Loi giai toi uu da cong bo, doc tu "Optimal value" trong dong COMMENT. */
    private final double optimalValue;

    /** Kho o chi so 0, cac diem giao tu 1 tro di — dung quy uoc chung cua do an. */
    private final List<OptimizationPoint> points;

    /** Ma tran EUC_2D da lam tron ve so nguyen theo quy uoc CVRPLIB. */
    private final double[][] matrix;

    public VrpInstance(String name, int dimension, double capacity, int vehicleCount,
                       double optimalValue, List<OptimizationPoint> points, double[][] matrix) {
        this.name = name;
        this.dimension = dimension;
        this.capacity = capacity;
        this.vehicleCount = vehicleCount;
        this.optimalValue = optimalValue;
        this.points = points;
        this.matrix = matrix;
    }

    /** So diem giao, tuc DIMENSION tru kho — con so cot "Số điểm" cua bang chuong 4. */
    public int deliveryPointCount() {
        return points.size() - 1;
    }

    /**
     * Danh sach diem giao (bo kho o dau), tra ve BAN SAO MOI moi lan goi.
     *
     * Phai la ban sao vi bo giai ghi thu tu diem vao doi tuong tuyen cua no. Chay lai
     * nhieu lan tren cung mot danh sach thi lan sau nhan dau vao da bi lan truoc dong vao.
     */
    public List<OptimizationPoint> deliveryPoints() {
        return new ArrayList<>(points.subList(1, points.size()));
    }

    /**
     * Doi xe cua bai toan: dung so xe ma de bai quy dinh, moi xe suc chua CAPACITY.
     *
     * Dung DUNG so xe do la dieu kien de so sanh voi loi giai toi uu cong bo — loi giai
     * do tim tren dung bay nhieu xe. Cho them xe la bai toan khac, con so gap khong con
     * doi chieu duoc voi tai lieu goc nua.
     */
    public List<OptimizationVehicle> fleet() {
        return fleetOf(vehicleCount);
    }

    /** Doi xe voi so luong tu chon — dung khi can do lai voi doi xe rong hon. */
    public List<OptimizationVehicle> fleetOf(int count) {
        List<OptimizationVehicle> vehicles = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            vehicles.add(new OptimizationVehicle((long) i, "Xe " + i, capacity, UNCONSTRAINED_VOLUME));
        }
        return vehicles;
    }

    /** Tong nhu cau cua ca bai — dung de biet doi xe co du suc chua khong. */
    public double totalDemand() {
        double total = 0;
        for (OptimizationPoint point : points) {
            total += point.getWeight();
        }
        return total;
    }
}
