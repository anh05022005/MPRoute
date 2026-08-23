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
    private static final long[][] DISTANCE_MATRIX = {
            /* tu KHO */ {0, 1000, 2000, 1000, 2000},
            /* tu A   */ {1000, 0, 1000, 2000, 3000},
            /* tu B   */ {2000, 1000, 0, 3000, 4000},
            /* tu C   */ {1000, 2000, 3000, 0, 1000},
            /* tu D   */ {2000, 3000, 4000, 1000, 0}
    };

    // Kho khong co nhu cau. Bon khach moi nguoi 6 thung
    private static final long[] DEMANDS = {0, 6, 6, 6, 6};

    // Hai xe, moi xe cho duoc 15 thung.
    // Tong nhu cau 24 > 15 nen bat buoc phai chia hai xe;
    // 6+6+6 = 18 > 15 nen moi xe chi cho duoc dung hai khach
    private static final long[] CAPACITIES = {15, 15};

    private static final int VEHICLE_COUNT = 2;
    private static final int DEPOT_INDEX = 0;

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
    private static final long OPTIMAL_TOTAL_METERS = 8000;

    @Test
    void orToolsSolvesSmallCvrpToHandCalculatedOptimum() {
        Loader.loadNativeLibraries();

        RoutingIndexManager indexManager =
                new RoutingIndexManager(DISTANCE_MATRIX.length, VEHICLE_COUNT, DEPOT_INDEX);
        RoutingModel model = new RoutingModel(indexManager);

        // Ham chi phi cung: tra thang vao ma tran da nap san trong bo nho.
        // Khong goi OSRM hay DB o day — day la vong lap nong nhat cua bo giai
        int distanceCallbackIndex = model.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = indexManager.indexToNode(fromIndex);
            int toNode = indexManager.indexToNode(toIndex);
            return DISTANCE_MATRIX[fromNode][toNode];
        });
        model.setArcCostEvaluatorOfAllVehicles(distanceCallbackIndex);

        // Rang buoc suc chua
        int demandCallbackIndex = model.registerUnaryTransitCallback(
                (long fromIndex) -> DEMANDS[indexManager.indexToNode(fromIndex)]);
        model.addDimensionWithVehicleCapacity(
                demandCallbackIndex,
                0,             // khong cho cho phep vuot
                CAPACITIES,
                true,          // bat dau tich luy tu 0
                "Capacity");

        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .build();

        Assignment solution = model.solveWithParameters(searchParameters);

        assertTrue(solution != null, "OR-Tools khong tim duoc loi giai nao");
        assertEquals(OPTIMAL_TOTAL_METERS, solution.objectiveValue(),
                "Tong quang duong phai bang dap so tinh tay 8000 m");

        // Doc lai tung tuyen de kiem hai viec: co ve kho, va khong vuot suc chua
        long manualTotal = 0;
        for (int vehicle = 0; vehicle < VEHICLE_COUNT; vehicle++) {
            List<Integer> route = new ArrayList<>();
            long routeLoad = 0;

            long index = model.start(vehicle);
            while (!model.isEnd(index)) {
                int node = indexManager.indexToNode(index);
                route.add(node);
                routeLoad += DEMANDS[node];

                long previousIndex = index;
                index = solution.value(model.nextVar(index));
                manualTotal += model.getArcCostForVehicle(previousIndex, index, vehicle);
            }
            // isEnd() la diem ket thuc cua xe, chinh la kho
            route.add(indexManager.indexToNode(index));

            assertEquals(4, route.size(),
                    "Moi tuyen phai la KHO -> 2 khach -> KHO, tuc 4 phan tu");
            assertEquals(DEPOT_INDEX, route.get(0), "Tuyen phai xuat phat tu kho");
            assertEquals(DEPOT_INDEX, route.get(route.size() - 1),
                    "Tuyen phai ket thuc o kho — day la loi kinh dien CBR-01");
            assertTrue(routeLoad <= CAPACITIES[vehicle],
                    "Xe " + vehicle + " cho " + routeLoad + " thung, vuot suc chua "
                            + CAPACITIES[vehicle]);
        }

        // Tu cong lai tung chang phai ra dung so bo giai bao — chung to
        // objectiveValue() da bao gom chang cuoi ve kho
        assertEquals(OPTIMAL_TOTAL_METERS, manualTotal,
                "Cong tay tung chang phai khop objectiveValue()");
    }
}
