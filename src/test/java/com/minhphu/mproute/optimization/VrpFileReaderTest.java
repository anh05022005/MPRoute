package com.minhphu.mproute.optimization;

import com.minhphu.mproute.service.optimization.VrpFileReader;
import com.minhphu.mproute.service.optimization.VrpInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem phan doc file .vrp cua bo chuan Augerat (CN-09-02).
 *
 * VI SAO VIET THEM HAI TEST NAY ngoai 14 test bat buoc: quy uoc EUC_2D lam tron ve so
 * nguyen la cho de sai ma sai thi KHONG BAO GIO BAO LOI — chuong trinh van chay, van in
 * ra bang, chi co moi con so gap cua ca chuong 4 la sai. Doc chi mot bang so sai vao bao
 * cao la mat diem nang hon nhieu so voi thoi gian viet hai test nay.
 */
class VrpFileReaderTest {

    @Test
    @DisplayName("Doc dung tieu de va lam tron khoang cach theo quy uoc EUC_2D")
    void readerParsesHeaderAndRoundsDistancesLikeCvrplib() {

        VrpInstance instance = VrpFileReader.readInstance("A-n32-k5");

        // Bon con so nay doc thang tu file, doi chieu duoc bang mat thuong
        assertEquals("A-n32-k5", instance.getName());
        assertEquals(32, instance.getDimension());
        assertEquals(100.0, instance.getCapacity());
        assertEquals(5, instance.getVehicleCount());
        assertEquals(784.0, instance.getOptimalValue());

        // DIMENSION dem ca kho, so diem GIAO thi bot mot
        assertEquals(31, instance.deliveryPointCount());

        /*
         * TINH TAY — day la cho de sai nhat cua ca lop VrpFileReader.
         *
         * Diem 1 (kho) o (82, 76), diem 2 o (96, 44):
         *     dx = 82 - 96 = -14
         *     dy = 76 - 44 =  32
         *     can_bac_hai(14^2 + 32^2) = can_bac_hai(196 + 1024) = can_bac_hai(1220)
         *                              = 34.9285...
         *     lam tron ve so nguyen gan nhat -> 35
         *
         * Giu nguyen 34.9285 thay vi 35 thi ca bang gap lech vai phan tram, va lech theo
         * kieu khong ai nhin ra duoc tu ket qua.
         */
        assertEquals(35.0, instance.getMatrix()[0][1]);

        // Kho luon o chi so 0 va khong co nhu cau — quy uoc chung cua ca do an
        assertTrue(instance.getPoints().get(0).isDepot());
        assertEquals(0.0, instance.getPoints().get(0).getWeight());

        /*
         * Ma tran phai DOI XUNG. 2-opt dung cong thuc do lech bon canh, va cong thuc do
         * chi dung khi D[i][j] = D[j][i]. Voi Euclid thi hien nhien doi xung, nhung kiem
         * mot cap la chan duoc truong hop sau nay co ai do doi cach tinh.
         */
        assertEquals(instance.getMatrix()[3][7], instance.getMatrix()[7][3]);
    }

    @Test
    @DisplayName("Ca muoi bai cua chuong 4 deu doc duoc va co loi giai toi uu da cong bo")
    void everyBenchmarkInstanceLoadsWithItsPublishedOptimum() {

        // Muoi bai nay la du lieu cua bang chuong 4. Thieu mot file la ca bang khong dung
        String[] instanceNames = {
                "A-n32-k5", "A-n39-k5", "A-n45-k6", "A-n60-k9", "A-n80-k10",
                "B-n35-k5", "B-n50-k7",
                "P-n40-k5", "P-n55-k7", "P-n76-k5"
        };

        for (String instanceName : instanceNames) {
            VrpInstance instance = VrpFileReader.readInstance(instanceName);

            assertEquals(instanceName, instance.getName(), "Ten trong file khac ten file");
            assertTrue(instance.getOptimalValue() > 0,
                    instanceName + " khong doc duoc loi giai toi uu tu dong COMMENT");
            assertTrue(instance.getVehicleCount() > 0,
                    instanceName + " khong doc duoc so xe tu dong COMMENT");

            // DIMENSION o tieu de phai khop so diem thuc su doc duoc
            assertEquals(instance.getDimension(), instance.getPoints().size(),
                    instanceName + " doc thieu diem so voi DIMENSION");

            /*
             * Doi xe cua de bai phai du cho tong nhu cau, khong thi bai nao cung "sot diem"
             * va ca bang gap thanh vo nghia. Rat sat: A-n45-k6 chi du thua 7 tren 600.
             */
            assertTrue(instance.getVehicleCount() * instance.getCapacity() >= instance.totalDemand(),
                    instanceName + " co tong nhu cau vuot suc chua ca doi xe");
        }
    }
}
