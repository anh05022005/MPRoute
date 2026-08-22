package com.minhphu.mproute.service.optimization;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    /** Dung cho kho. */
    public static OptimizationPoint depot(double latitude, double longitude) {
        OptimizationPoint diem = new OptimizationPoint();
        diem.setName("KHO");
        diem.setLatitude(latitude);
        diem.setLongitude(longitude);
        diem.setDepot(true);
        diem.setMatrixIndex(0);
        return diem;
    }

    /** Dung cho mot don hang can giao. */
    public static OptimizationPoint cuaDon(Long orderId, String name,
                                           double latitude, double longitude,
                                           double weight, double volume) {
        OptimizationPoint diem = new OptimizationPoint();
        diem.setOrderId(orderId);
        diem.setName(name);
        diem.setLatitude(latitude);
        diem.setLongitude(longitude);
        diem.setWeight(weight);
        diem.setVolume(volume);
        return diem;
    }

    @Override
    public String toString() {
        return (depot ? "KHO" : name + "#" + orderId)
                // Locale.ROOT de dau thap phan luon la dau cham, khong doi theo mui vung
                + String.format(java.util.Locale.ROOT, "[%d] (%.5f, %.5f) %.1fkg %.3fm3",
                        matrixIndex, latitude, longitude, weight, volume);
    }
}
