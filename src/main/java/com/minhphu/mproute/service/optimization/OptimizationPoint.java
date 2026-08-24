package com.minhphu.mproute.service.optimization;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Mot diem trong bai toan phan tuyen: hoac la KHO, hoac la mot don hang can giao.
 *
 * Lop nay CO Y tach khoi entity Order va Customer. Thuat toan chi can sau con so
 * (toa do, kg, m3, chi so trong ma tran) chu khong can biet gi ve JPA. Nho vay
 * chay duoc bang main() khong can MySQL, va chay duoc bo benchmark Augerat von
 * khong co khach hang nao ca.
 */
@Getter
@Setter
@NoArgsConstructor
public class OptimizationPoint {

    // null neu la kho
    private Long orderId;

    // "KHO" hoac ten khach hang, chi dung de in ra cho de doc
    private String name;

    private double latitude;
    private double longitude;

    // Nhu cau cua diem. Kho luon bang 0
    private double weight;   // kg
    private double volume;   // m3

    /*
     * Lombok sinh getter isDepot() va setter setDepot() cho truong nay.
     * Dat ten truong la "depot" chu khong phai "isDepot" de dung chuan JavaBeans.
     */
    private boolean depot;

    /*
     * Vi tri cua diem trong ma tran khoang cach: matrix[i][j] la khoang cach tu
     * diem co matrixIndex = i toi diem co matrixIndex = j.
     *
     * QUY UOC XUYEN SUOT DO AN: chi so 0 LUON la kho, cac diem giao bat dau tu 1.
     * DistanceMatrixService dat gia tri nay khi nap ma tran.
     */
    private int matrixIndex;

    /*
     * Gio du kien den diem nay (CBR-13), do EtaCalculator dat sau khi da co thu tu diem.
     *
     * De null cho toi luc do chu khong dat gia tri mac dinh: tuyen vua sinh ra ma chua
     * tinh ETA thi man hinh phai hien "chua tinh", khong duoc hien mot gio bia ra trong
     * khi thuc te chua tinh gio nao ca.
     */
    private LocalDateTime estimatedArrival;

    /** Dung cho kho. */
    public static OptimizationPoint depot(double latitude, double longitude) {
        OptimizationPoint point = new OptimizationPoint();
        point.setName("KHO");
        point.setLatitude(latitude);
        point.setLongitude(longitude);
        point.setDepot(true);
        point.setMatrixIndex(0);
        return point;
    }

    /** Dung cho mot don hang can giao. */
    public static OptimizationPoint forOrder(Long orderId, String name,
                                             double latitude, double longitude,
                                             double weight, double volume) {
        OptimizationPoint point = new OptimizationPoint();
        point.setOrderId(orderId);
        point.setName(name);
        point.setLatitude(latitude);
        point.setLongitude(longitude);
        point.setWeight(weight);
        point.setVolume(volume);
        return point;
    }

    @Override
    public String toString() {
        return (depot ? "KHO" : name + "#" + orderId)
                // Locale.ROOT de dau thap phan luon la dau cham, khong doi theo mui vung
                + String.format(Locale.ROOT, "[%d] (%.5f, %.5f) %.1fkg %.3fm3",
                        matrixIndex, latitude, longitude, weight, volume);
    }
}
