package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.EtaCalculator;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test bat buoc cho ETA (CBR-13).
 *
 * Con so trong test nay chinh la vi du tinh tay o docs/03 muc 9, chep sang de doi chieu.
 * Hoi dong hoi "sao em biet 08:42 la dung" thi day la cho tra loi.
 */
class EtaCalculatorTest {

    /*
     * Bon diem, khoang cach dat sao cho quang duong tich luy ra so tron:
     *
     *     KHO -> P1 : 12 000 m   -> tich luy 12 000
     *     P1  -> P2 :  1 300 m   -> tich luy 13 300   (dung con so cua docs/03)
     *     P2  -> P3 :  6 700 m   -> tich luy 20 000
     *     P3  -> KHO: 15 000 m
     *
     * Ma tran doi xung, cac o khong dung toi dat mot gia tri bat ky cho du hinh.
     */
    private static final double[][] MATRIX = {
            /* tu KHO */ {     0, 12000, 13000, 15000},
            /* tu P1  */ { 12000,     0,  1300,  8000},
            /* tu P2  */ { 13000,  1300,     0,  6700},
            /* tu P3  */ { 15000,  8000,  6700,     0}
    };

    private static final double SPEED_KMH = 30.0;
    private static final int LOADING_MINUTES_PER_STOP = 15;

    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 8, 25, 8, 0);

    private OptimizationPoint point(int matrixIndex, String name) {
        OptimizationPoint point = OptimizationPoint.forOrder(
                (long) matrixIndex, name, 0, 0, 100, 1);
        point.setMatrixIndex(matrixIndex);
        return point;
    }

    @Test
    void etaOfFirstStopMatchesHandCalculation() {

        OptimizationRoute route = new OptimizationRoute(
                new OptimizationVehicle(1L, "29H-123.45", 1500, 12));
        route.addPoint(point(1, "P1"));
        route.addPoint(point(2, "P2"));
        route.addPoint(point(3, "P3"));

        // Chua tinh thi phai la null, khong duoc bia ra mot gio nao ca
        assertNull(route.getPoints().get(0).getEstimatedArrival());

        EtaCalculator.assignEta(route, MATRIX, DEPARTURE, SPEED_KMH, LOADING_MINUTES_PER_STOP);

        /*
         * ================= DIEM 1 — cho de sai nhat =================
         *
         * Quang duong tich luy: 12 000 m = 12 km
         * Di chuyen : 12 / 30 x 60 = 24 phut
         * Boc do    : 0 phut  <-- chua giao diem nao truoc do, KHONG duoc tinh 15 phut
         * ETA       : 08:00 + 24 = 08:24
         *
         * Nhan he so boc do voi (i + 1) thay vi i la ra 08:39 — sai 15 phut ngay tu diem
         * dau, va moi diem sau lech thang 15 phut.
         */
        assertEquals(LocalDateTime.of(2026, 8, 25, 8, 24),
                route.getPoints().get(0).getEstimatedArrival(),
                "ETA diem dau = 08:00 + 12km / 30km/h = 08:24, chua co boc do");

        /*
         * ================= DIEM 2 — vi du cua docs/03 muc 9 =================
         *
         * Quang duong tich luy: 12 000 + 1 300 = 13 300 m = 13.3 km
         * Di chuyen : 13.3 / 30 x 60 = 26.6 phut = 26 phut 36 giay
         * Boc do    : 1 x 15 = 15 phut     (da giao xong diem 1)
         * ETA       : 08:00 + 26 phut 36 giay + 15 phut = 08:41:36
         *
         * Cong bang GIAY chu khong lam tron phut, khong thi 0.6 phut cua moi chang cong
         * don lai thanh sai lech vai phut o cuoi tuyen.
         */
        assertEquals(LocalDateTime.of(2026, 8, 25, 8, 41, 36),
                route.getPoints().get(1).getEstimatedArrival(),
                "ETA diem 2 = 08:00 + 26.6 phut + 15 phut = 08:41:36");

        /*
         * ================= DIEM 3 =================
         *
         * Quang duong tich luy: 13 300 + 6 700 = 20 000 m = 20 km
         * Di chuyen : 20 / 30 x 60 = 40 phut
         * Boc do    : 2 x 15 = 30 phut
         * ETA       : 08:00 + 70 phut = 09:10
         */
        assertEquals(LocalDateTime.of(2026, 8, 25, 9, 10),
                route.getPoints().get(2).getEstimatedArrival(),
                "ETA diem 3 = 08:00 + 40 phut + 30 phut = 09:10");

        /*
         * ================= VE TOI KHO (CBR-01) =================
         *
         * Xe con phai do hang o diem cuoi roi moi chay ve:
         *   09:10 + 15 phut boc do + (15 km / 30 km/h = 30 phut) = 09:55
         *
         * Con so nay de canh bao dieu phoi vien khi tuyen keo qua gio lam viec.
         */
        assertEquals(LocalDateTime.of(2026, 8, 25, 9, 55),
                EtaCalculator.estimatedReturnToDepot(route, MATRIX, SPEED_KMH, LOADING_MINUTES_PER_STOP),
                "Ve toi kho = 09:10 + 15 phut boc do + 30 phut chay ve = 09:55");
    }
}
