package com.minhphu.mproute.optimization;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem chung OR-Tools chay duoc tren may nay truoc khi dua vao he thong.
 *
 * Kiem ba viec:
 *   1. Loader.loadNativeLibraries() nap duoc file .dll tren Windows + JDK 23
 *   2. Bo giai ton trong rang buoc suc chua
 *   3. Tong quang duong CO chang cuoi ve kho (CBR-01)
 *
 * Du lieu dat tren mot truc thang de tinh tay ra dap so, khong sinh ngau nhien.
 */
class OrToolsSmokeTest {

    /*
     * Nam diem nam tren truc hoanh, don vi 1000 m:
     *
     *      D(-2)   C(-1)   KHO(0)   A(1)   B(2)
     *
     * Khoang cach giua hai diem = hieu toa do. Bang duoi day chep tay tu hinh tren.
     */
    private static final long[][] MA_TRAN = {
            /* tu KHO */ {0, 1000, 2000, 1000, 2000},
            /* tu A   */ {1000, 0, 1000, 2000, 3000},
            /* tu B   */ {2000, 1000, 0, 3000, 4000},
            /* tu C   */ {1000, 2000, 3000, 0, 1000},
            /* tu D   */ {2000, 3000, 4000, 1000, 0}
    };

    // Kho khong co nhu cau. Bon khach moi nguoi 6 thung
    private static final long[] NHU_CAU = {0, 6, 6, 6, 6};

    // Hai xe, moi xe cho duoc 15 thung.
    // Tong nhu cau 24 > 15 nen bat buoc phai chia hai xe;
    // 6+6+6 = 18 > 15 nen moi xe chi cho duoc dung hai khach
    private static final long[] SUC_CHUA = {15, 15};

    private static final int SO_XE = 2;
    private static final int CHI_SO_KHO = 0;

    /*
     * Dap so tinh tay:
     *   Xe 1: KHO -> A -> B -> KHO = 1000 + 1000 + 2000 = 4000
     *   Xe 2: KHO -> C -> D -> KHO = 1000 + 1000 + 2000 = 4000
     *   Tong = 8000 m
     *
     * Moi cach chia khac deu dai hon, vi du ghep {A,C} va {B,D}:
     *   KHO -> A -> C -> KHO = 1000 + 2000 + 1000 = 4000
     *   KHO -> B -> D -> KHO = 2000 + 4000 + 2000 = 8000
     *   Tong = 12000 m
     */
    private static final long TONG_TOI_UU = 8000;

    @Test
    void orToolsGiaiDungBaiCvrpNhoTinhTayDuoc() {
        Loader.loadNativeLibraries();

        RoutingIndexManager quanLyChiSo = new RoutingIndexManager(MA_TRAN.length, SO_XE, CHI_SO_KHO);
        RoutingModel moHinh = new RoutingModel(quanLyChiSo);

        // Ham chi phi cung: tra thang vao ma tran da nap san trong bo nho.
        // Khong goi OSRM hay DB o day — day la vong lap nong nhat cua bo giai
        int chiSoHamKhoangCach = moHinh.registerTransitCallback((long tuChiSo, long denChiSo) -> {
            int tuDiem = quanLyChiSo.indexToNode(tuChiSo);
            int denDiem = quanLyChiSo.indexToNode(denChiSo);
            return MA_TRAN[tuDiem][denDiem];
        });
        moHinh.setArcCostEvaluatorOfAllVehicles(chiSoHamKhoangCach);

        // Rang buoc suc chua
        int chiSoHamNhuCau = moHinh.registerUnaryTransitCallback(
                (long tuChiSo) -> NHU_CAU[quanLyChiSo.indexToNode(tuChiSo)]);
        moHinh.addDimensionWithVehicleCapacity(
                chiSoHamNhuCau,
                0,             // khong cho cho phep vuot
                SUC_CHUA,
                true,          // bat dau tich luy tu 0
                "SucChua");

        RoutingSearchParameters thamSo = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .build();

        Assignment ketQua = moHinh.solveWithParameters(thamSo);

        assertTrue(ketQua != null, "OR-Tools khong tim duoc loi giai nao");
        assertEquals(TONG_TOI_UU, ketQua.objectiveValue(),
                "Tong quang duong phai bang dap so tinh tay 8000 m");

        // Doc lai tung tuyen de kiem hai viec: co ve kho, va khong vuot suc chua
        long tongTuCong = 0;
        for (int xe = 0; xe < SO_XE; xe++) {
            List<Integer> tuyen = new ArrayList<>();
            long taiTrongTuyen = 0;

            long chiSo = moHinh.start(xe);
            while (!moHinh.isEnd(chiSo)) {
                int diem = quanLyChiSo.indexToNode(chiSo);
                tuyen.add(diem);
                taiTrongTuyen += NHU_CAU[diem];

                long chiSoTruoc = chiSo;
                chiSo = ketQua.value(moHinh.nextVar(chiSo));
                tongTuCong += moHinh.getArcCostForVehicle(chiSoTruoc, chiSo, xe);
            }
            // isEnd() la diem ket thuc cua xe, chinh la kho
            tuyen.add(quanLyChiSo.indexToNode(chiSo));

            assertEquals(4, tuyen.size(),
                    "Moi tuyen phai la KHO -> 2 khach -> KHO, tuc 4 phan tu");
            assertEquals(CHI_SO_KHO, tuyen.get(0), "Tuyen phai xuat phat tu kho");
            assertEquals(CHI_SO_KHO, tuyen.get(tuyen.size() - 1),
                    "Tuyen phai ket thuc o kho — day la loi kinh dien CBR-01");
            assertTrue(taiTrongTuyen <= SUC_CHUA[xe],
                    "Xe " + xe + " cho " + taiTrongTuyen + " thung, vuot suc chua " + SUC_CHUA[xe]);
        }

        // Tu cong lai tung chang phai ra dung so bo giai bao — chung to
        // objectiveValue() da bao gom chang cuoi ve kho
        assertEquals(TONG_TOI_UU, tongTuCong,
                "Cong tay tung chang phai khop objectiveValue()");
    }
}
