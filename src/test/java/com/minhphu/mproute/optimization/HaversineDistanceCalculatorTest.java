package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.HaversineDistanceCalculator;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiem cong thuc Haversine — test bat buoc so 2 trong docs/10-quy-uoc-ky-thuat.md.
 *
 * JUnit thuan, khong dung Spring context, khong can MySQL, khong can OSRM.
 * Du lieu cung, dap so tinh tay duoc, khong sinh ngau nhien.
 */
class HaversineDistanceCalculatorTest {

    /*
     * Toa do that lay tu docs/08-du-lieu-mau.md:
     *   Kho Minh Phu, duong Phan Trong Tue, Thanh Tri
     *   Cua hang Bao Chau, 91 Chua Boc, Dong Da
     */
    private static final double DEPOT_LAT = 20.9497;
    private static final double DEPOT_LON = 105.8172;
    private static final double CUSTOMER_LAT = 21.0067;
    private static final double CUSTOMER_LON = 105.8290;

    private static final double DETOUR_FACTOR = 1.3;

    @Test
    void haversineMatchesHandCalculatedDistances() {

        /*
         * TRUONG HOP 1 — mot do vi do, tinh tay duoc bang giay but.
         *
         * Hai diem cung kinh do 0, cach nhau dung 1 do vi do. Khi kinh do bang nhau
         * thi deltaLambda = 0, so hang thu hai cua cong thuc bien mat, con lai:
         *
         *     a = sin(deltaPhi/2)^2   ->   c = 2 * arcsin(sin(deltaPhi/2)) = deltaPhi
         *     khoang cach = R * deltaPhi = 6371000 * (pi/180)
         *                 = 6371000 * 0.01745329252 = 111194.93 m
         *
         * Day la con so quen thuoc: mot do vi do bang khoang 111,2 km.
         */
        double oneDegreeOfLatitude = HaversineDistanceCalculator.greatCircleDistance(0.0, 0.0, 1.0, 0.0);
        assertEquals(111194.93, oneDegreeOfLatitude, 0.01,
                "Mot do vi do phai bang R * pi/180 = 111194.93 m");

        /*
         * TRUONG HOP 2 — hai diem that cua do an, kho va mot khach hang.
         *
         * Thay so vao cong thuc:
         *   deltaPhi    = radian(21.0067 - 20.9497) = radian(0.0570)  = 0.00099484
         *   deltaLambda = radian(105.8290 - 105.8172) = radian(0.0118) = 0.00020595
         *   phi1 = radian(20.9497) = 0.36564,  phi2 = radian(21.0067) = 0.36663
         *
         *   a = sin(0.00049742)^2 + cos(0.36564)*cos(0.36663)*sin(0.000102974)^2
         *     = 2.47427e-7 + 0.87055 * 1.06035e-8
         *     = 2.56659e-7
         *   c = 2 * atan2(sqrt(a), sqrt(1-a)) = 1.01326e-3
         *   khoang cach = 6371000 * 1.01326e-3 = 6455.43 m
         *
         * Doi chieu doc lap bang cach coi mat dat la phang o khoang cach nay:
         *   ngang = 0.0118 do kinh * 111194.93 * cos(20.978 do) = 1225.4 m
         *   doc   = 0.0570 do vi  * 111194.93                   = 6338.1 m
         *   can(1225.4^2 + 6338.1^2) = 6455.43 m  ->  khop
         */
        double straightLine = HaversineDistanceCalculator.greatCircleDistance(
                DEPOT_LAT, DEPOT_LON, CUSTOMER_LAT, CUSTOMER_LON);
        assertEquals(6455.43, straightLine, 0.5,
                "Kho -> 91 Chua Boc theo duong chim bay phai la 6455,43 m");

        /*
         * TRUONG HOP 3 — cung hai diem do nhung qua interface, tuc la DA nhan he so.
         * 6455.43 * 1.3 = 8392.06 m. He so 1.3 la vi xe phai di theo pho chu khong
         * bay thang duoc (CBR-11).
         */
        OptimizationPoint depot = OptimizationPoint.depot(DEPOT_LAT, DEPOT_LON);
        OptimizationPoint customer = OptimizationPoint.forOrder(
                1L, "Cua hang Bao Chau", CUSTOMER_LAT, CUSTOMER_LON, 6.0, 0.085);

        HaversineDistanceCalculator calculator = new HaversineDistanceCalculator(DETOUR_FACTOR);

        double approximateRoadDistance = calculator.distanceBetween(depot, customer);
        assertEquals(8392.06, approximateRoadDistance, 0.5,
                "Nhan he so 1.3 phai ra 8392,06 m");

        /*
         * TRUONG HOP 4 — Haversine doi xung san, di va ve bang nhau.
         * Ghi lai day de thay ro su khac biet voi OSRM: OSRM tra ve ma tran BAT doi xung
         * vi duong mot chieu, do la ly do DistanceMatrixService phai doi xung hoa o buoc B3.
         */
        double reverseDirection = calculator.distanceBetween(customer, depot);
        assertEquals(approximateRoadDistance, reverseDirection, 1e-9,
                "Haversine phai doi xung: di va ve bang nhau");

        // Di tu mot diem toi chinh no phai bang 0
        assertEquals(0.0, calculator.distanceBetween(depot, depot), 1e-9);
    }
}
