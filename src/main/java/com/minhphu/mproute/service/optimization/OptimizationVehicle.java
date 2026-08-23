package com.minhphu.mproute.service.optimization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mot chiec xe trong bai toan phan tuyen.
 *
 * HAI rang buoc suc chua di cung nhau, khong bao gio duoc kiem mot cai (CBR-03, CBR-04):
 * hang cua Minh Phu co loai nhe ma cong kenh (bong tay trang 6 kg / 0.085 m3) va loai
 * nang ma gon (nuoc lau san 24.8 kg / 0.031 m3). Xe cho toan bong tay trang se DAY CHO
 * truoc khi day tai — chi kiem kg thi he thong tuong van con cho.
 *
 * Giong OptimizationPoint, lop nay khong biet gi ve JPA de con chay duoc bang main()
 * va chay duoc bo benchmark Augerat.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationVehicle {

    private Long vehicleId;
    private String licensePlate;

    private double maxWeight;   // kg
    private double maxVolume;   // m3

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s (%.0f kg / %.1f m3)",
                licensePlate, maxWeight, maxVolume);
    }
}
