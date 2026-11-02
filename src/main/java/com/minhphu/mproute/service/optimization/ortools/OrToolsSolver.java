package com.minhphu.mproute.service.optimization.ortools;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
import com.google.protobuf.Duration;
import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.OptimizationVehicle;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;
import com.minhphu.mproute.service.optimization.RoutingAlgorithm;
import com.minhphu.mproute.service.optimization.RoutingResult;

import java.util.ArrayList;
import java.util.List;

/**
 * BO GIAI CHINH cua he thong — Google OR-Tools Routing Library (docs/03 muc 5).
 *
 * Day la bo giai DUY NHAT: tuyen ma Minh Phu thuc su chay la ket qua cua lop nay. Hai
 * bo giai tu cai dat NN va NN + 2-opt da bo (docs/09 muc A7) — OR-Tools cho tuyen ngan
 * hon ro ret, giu them hai bo giai chi de so sanh thi phai bao tri gap doi.
 *
 * ==================================================================================
 * BON QUYET DINH PHAI GIAI THICH DUOC KHI VAN DAP — moi cai co comment tai cho:
 *   a) Vi sao phai quy doi sang so nguyen, va vi sao lam tron nhu cau LEN / suc chua XUONG
 *   b) Vi sao chang cuoi ve kho tu co, khong phai cong them (CBR-01)
 *   c) Vi sao can addDisjunction thay vi de bo giai tra ve null
 *   d) Vi sao bat buoc dat gioi han thoi gian
 * ==================================================================================
 */
public class OrToolsSolver implements RoutingAlgorithm {

    /** Chi so cua kho trong mo hinh OR-Tools, trung voi quy uoc chung cua do an. */
    private static final int DEPOT_NODE = RouteDistanceCalculator.DEPOT_INDEX;

    /*
     * (a) HE SO QUY DOI SANG SO NGUYEN.
     *
     * OR-Tools lam viec voi long, khong nhan double — cac Dimension cua no cong don
     * bang so nguyen. Nen phai quy doi:
     *     khoi luong: kg x 1 000     -> gam
     *     the tich  : m3 x 1 000 000 -> cm3
     *     khoang cach da la met, chi can lam tron
     *
     * Chon don vi nho nhu vay de sai so lam tron khong dang ke: 1 gam tren tai trong
     * 1500 kg la mot phan mot trieu rui.
     */
    private static final double WEIGHT_SCALE = 1_000.0;        // kg -> g
    private static final double VOLUME_SCALE = 1_000_000.0;    // m3 -> cm3

    /** Gioi han thoi gian mac dinh cho mot lan giai, giay. */
    private static final long DEFAULT_TIME_LIMIT_SECONDS = 5;

    /**
     * Thu vien native (.dll tren Windows) chi duoc nap MOT lan cho ca tien trinh.
     * Nap lai lan hai la loi UnsatisfiedLinkError, nen chot bang bien static.
     */
    private static boolean nativeLibrariesLoaded = false;

    private final long timeLimitSeconds;

    public OrToolsSolver() {
        this(DEFAULT_TIME_LIMIT_SECONDS);
    }

    /** Dung cho unit test va cho benchmark, noi can dat gioi han thoi gian khac. */
    public OrToolsSolver(long timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    private static synchronized void loadNativeLibrariesOnce() {
        if (!nativeLibrariesLoaded) {
            Loader.loadNativeLibraries();
            nativeLibrariesLoaded = true;
        }
    }

    @Override
    public RoutingResult solve(List<OptimizationPoint> deliveryPoints,
                               List<OptimizationVehicle> vehicles,
                               double[][] matrix) {

        long startedAt = System.currentTimeMillis();

        RoutingResult result = new RoutingResult();
        result.setAlgorithm(algorithmType());

        // Khong co diem hoac khong co xe thi khong co gi de giai
        if (deliveryPoints.isEmpty() || vehicles.isEmpty()) {
            result.setUnassignedPoints(new ArrayList<>(deliveryPoints));
            result.setExecutionTimeMs(System.currentTimeMillis() - startedAt);
            return result;
        }

        loadNativeLibrariesOnce();

        int nodeCount = deliveryPoints.size() + 1;   // + 1 la kho
        int vehicleCount = vehicles.size();

        /*
         * BANG TRA CHI SO: node cua OR-Tools -> chi so trong ma tran cua do an.
         *
         * Node 0 la kho, node 1..n la deliveryPoints theo dung thu tu truyen vao.
         * Khong gia dinh matrixIndex chay lien tuc 1, 2, 3...: neu sau nay man hinh
         * phan tuyen loc bot vai don ra khoi lan chay thi cac chi so con lai se thua,
         * gia dinh "node = matrixIndex" luc do sai lang le va rat kho truy.
         */
        int[] matrixIndexOfNode = new int[nodeCount];
        matrixIndexOfNode[0] = DEPOT_NODE;
        for (int node = 1; node < nodeCount; node++) {
            matrixIndexOfNode[node] = deliveryPoints.get(node - 1).getMatrixIndex();
        }

        /*
         * (b) CHANG CUOI VE KHO TU CO (CBR-01).
         *
         * RoutingIndexManager dung mot chi so kho duy nhat cho ca diem xuat phat lan
         * diem ket thuc cua MOI xe. Vi vay cung "diem cuoi -> kho" la mot phan cua mo
         * hinh chu khong phai thu ta cong them, va objectiveValue() da bao gom no.
         *
         * Tong cuoi cung van tinh lai bang RouteDistanceCalculator (xem cuoi ham), ham
         * tu cong chang cuoi, de moi con so cua he thong do bang cung mot thuoc do.
         */
        RoutingIndexManager indexManager =
                new RoutingIndexManager(nodeCount, vehicleCount, DEPOT_NODE);
        RoutingModel model = new RoutingModel(indexManager);

        // ---------- Ham chi phi: tra thang vao ma tran da nap san ----------
        /*
         * Ham nay bi goi HANG CHUC NGHIN lan trong mot lan giai. Tuyet doi khong duoc
         * goi OSRM hay truy van CSDL o day — do la dieu cam so 4 cua quy uoc ky thuat,
         * va no ap dung ca cho bo giai thu vien.
         */
        int distanceCallback = model.registerTransitCallback((fromIndex, toIndex) -> {
            int from = matrixIndexOfNode[indexManager.indexToNode(fromIndex)];
            int to = matrixIndexOfNode[indexManager.indexToNode(toIndex)];
            return Math.round(matrix[from][to]);
        });
        model.setArcCostEvaluatorOfAllVehicles(distanceCallback);

        // ---------- HAI rang buoc suc chua = HAI Dimension rieng ----------
        /*
         * DAY LA CHO DE SAI NHAT KHI DUNG OR-TOOLS: gan nhu moi vi du tren mang chi khai
         * mot Dimension ten "Capacity". Hang cua Minh Phu co loai nhe ma cong kenh (bong
         * tay trang 6 kg / 0.085 m3) va loai nang ma gon (nuoc lau san 24.8 kg / 0.031 m3),
         * nen chi khai mot chieu la xe cho toan bong tay trang day thung ma he thong van
         * tuong con cho (CBR-03, CBR-04).
         */
        long[] weightDemands = new long[nodeCount];
        long[] volumeDemands = new long[nodeCount];
        for (int node = 1; node < nodeCount; node++) {
            OptimizationPoint point = deliveryPoints.get(node - 1);
            /*
             * (a) LAM TRON NHU CAU LEN. Nguoc lai — lam tron xuong — thi mot don 10.4 gam
             * le thanh 10 gam, cong don ca tuyen co the sinh ra tuyen trong hop le tren
             * man hinh nhung thuc te qua tai vai tram gam.
             */
            weightDemands[node] = (long) Math.ceil(point.getWeight() * WEIGHT_SCALE);
            volumeDemands[node] = (long) Math.ceil(point.getVolume() * VOLUME_SCALE);
        }

        long[] weightCapacities = new long[vehicleCount];
        long[] volumeCapacities = new long[vehicleCount];
        for (int vehicle = 0; vehicle < vehicleCount; vehicle++) {
            // (a) LAM TRON SUC CHUA XUONG — cung mot ly do, nhung nguoc chieu
            weightCapacities[vehicle] =
                    (long) Math.floor(vehicles.get(vehicle).getMaxWeight() * WEIGHT_SCALE);
            volumeCapacities[vehicle] =
                    (long) Math.floor(vehicles.get(vehicle).getMaxVolume() * VOLUME_SCALE);
        }

        int weightCallback = model.registerUnaryTransitCallback(
                fromIndex -> weightDemands[indexManager.indexToNode(fromIndex)]);
        model.addDimensionWithVehicleCapacity(
                weightCallback,
                0,                  // khong cho phep vuot suc chua du chi mot chut
                weightCapacities,
                true,               // moi xe bat dau tich luy tu 0, tuc roi kho la xe rong
                "Weight");

        int volumeCallback = model.registerUnaryTransitCallback(
                fromIndex -> volumeDemands[indexManager.indexToNode(fromIndex)]);
        model.addDimensionWithVehicleCapacity(
                volumeCallback, 0, volumeCapacities, true, "Volume");

        // ---------- (c) Cho phep BO DIEM khi khong du suc chua ----------
        /*
         * Mac dinh OR-Tools bat MOI diem phai duoc tham. Tong suc chua khong du thi
         * solveWithParameters() tra ve null — va null thi khong phan biet duoc "thieu
         * suc chua" voi "thu vien loi", man hinh chi biet bao mot loi chung chung.
         *
         * addDisjunction bien viec bo mot diem thanh mot lua chon CO GIA: bo giai chi bo
         * khi that su khong con cach nao khac. Cac diem bi bo doc ra o duoi va do vao
         * unassignedPoints de hien canh bao CN-03-14 / MSG-08.
         *
         * TIEN PHAT phai LON HON quang duong xa nhat co the cua mot diem, khong thi bo
         * giai thay bo diem re hon la di giao. Lay 10 lan o lon nhat cua ma tran la du xa.
         *
         * Tien phat RIENG tung diem theo dropPriority (BR03-14): het cho thi bo giai bo
         * diem uu tien thap truoc, du diem do gan hay xa. Xem dropPenaltiesFor.
         */
        long[] dropPenalties = dropPenaltiesFor(deliveryPoints, matrix, vehicleCount);
        for (int node = 1; node < nodeCount; node++) {
            model.addDisjunction(new long[]{indexManager.nodeToIndex(node)}, dropPenalties[node - 1]);
        }

        // ---------- Tham so tim kiem ----------
        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters()
                .toBuilder()
                /*
                 * Loi giai ban dau: PATH_CHEAPEST_ARC — cung y tuong voi Nearest Neighbor,
                 * moi buoc noi them cung duong re nhat tu diem dang dung. Nhanh ra mot loi
                 * giai hop le de buoc sau co cai ma cai thien.
                 */
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                /*
                 * Cai thien: GUIDED_LOCAL_SEARCH. Ngoai dao doan ben trong mot tuyen
                 * (giong 2-opt), no con CHUYEN DIEM TU XE NAY SANG XE KHAC — tuc la sua
                 * ca cach chia don cho xe, cho ma heuristic tham lam hay sai nhat (docs/07).
                 */
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                /*
                 * (d) GIOI HAN THOI GIAN LA BAT BUOC. GUIDED_LOCAL_SEARCH la sieu tim
                 * kiem, no KHONG TU DUNG — cu cai thien mai cho toi khi het gio cho phep.
                 * Thieu dong nay la man hinh phan tuyen treo vinh vien.
                 */
                .setTimeLimit(Duration.newBuilder().setSeconds(timeLimitSeconds).build())
                .build();

        Assignment solution = model.solveWithParameters(searchParameters);

        /*
         * Voi addDisjunction thi hau nhu khong bao gio ra null nua, nhung van phai chan:
         * tra ve ket qua rong con hon nem NullPointerException ra man hinh phan tuyen.
         */
        if (solution == null) {
            result.setUnassignedPoints(new ArrayList<>(deliveryPoints));
            result.setExecutionTimeMs(System.currentTimeMillis() - startedAt);
            return result;
        }

        readRoutes(result, solution, model, indexManager, deliveryPoints, vehicles, matrix);
        readUnassignedPoints(result, solution, model, indexManager, deliveryPoints);

        /*
         * Tinh lai tong bang RouteDistanceCalculator, khong lay objectiveValue().
         *
         * Hai ly do: objectiveValue() la so nguyen da lam tron tung chang, va sau nay neu
         * them chi phi phu vao ham muc tieu thi no khong con la quang duong thuan nua.
         * Moc A, moc B va gap benchmark deu tinh bang ham do, phai cung mot thuoc do.
         */
        result.setOptimizedDistance(
                RouteDistanceCalculator.totalDistanceOfRoutes(result.getRoutes(), matrix));
        result.setExecutionTimeMs(System.currentTimeMillis() - startedAt);
        return result;
    }

    /** Doc thu tu diem cua tung xe tu loi giai ra thanh cac OptimizationRoute. */
    private void readRoutes(RoutingResult result, Assignment solution, RoutingModel model,
                            RoutingIndexManager indexManager,
                            List<OptimizationPoint> deliveryPoints,
                            List<OptimizationVehicle> vehicles,
                            double[][] matrix) {

        for (int vehicle = 0; vehicle < vehicles.size(); vehicle++) {

            OptimizationRoute route = new OptimizationRoute(vehicles.get(vehicle));

            /*
             * Di theo chuoi nextVar tu diem xuat phat cua xe cho toi khi gap diem ket thuc.
             * isEnd(index) chinh la luc xe ve toi kho, nen vong lap dung o do va kho khong
             * bi them vao danh sach diem — dung quy uoc "points khong chua kho".
             */
            long index = model.start(vehicle);
            while (!model.isEnd(index)) {
                int node = indexManager.indexToNode(index);
                if (node != DEPOT_NODE) {
                    route.addPoint(deliveryPoints.get(node - 1));
                }
                index = solution.value(model.nextVar(index));
            }

            // Xe khong duoc giao diem nao thi khong tao tuyen rong
            if (!route.isEmpty()) {
                RouteDistanceCalculator.updateTotalDistance(route, matrix);
                result.addRoute(route);
            }
        }
    }

    /**
     * Doc cac diem bi bo.
     *
     * DAU HIEU NHAN BIET: mot diem bi bo thi bien nextVar cua no tro vao CHINH NO —
     * no khong nam trong chuoi cua xe nao ca.
     */
    private void readUnassignedPoints(RoutingResult result, Assignment solution,
                                      RoutingModel model, RoutingIndexManager indexManager,
                                      List<OptimizationPoint> deliveryPoints) {

        List<OptimizationPoint> unassigned = new ArrayList<>();
        for (int node = 1; node <= deliveryPoints.size(); node++) {
            long index = indexManager.nodeToIndex(node);
            if (solution.value(model.nextVar(index)) == index) {
                unassigned.add(deliveryPoints.get(node - 1));
            }
        }
        // Danh sach RONG chu khong bao gio null — man hinh phan biet duoc hai truong hop
        result.setUnassignedPoints(unassigned);
    }

    /**
     * Tien phat khi bo tung diem = PHAN CO SO + BAC UU TIEN x dropPriority.
     *
     * PHAN CO SO = 10 lan o lon nhat cua ma tran. Phai lon hon quang duong xa nhat mot
     * diem co the gay ra (toi da la mot chuyen khu hoi rieng, tuc 2 lan o lon nhat). He so
     * 10 la de du xa: bo giai chi bo diem khi that su khong con cach nao xep vua, chu khong
     * bo vi thay re. Moi diem dropPriority = 0 thi tien phat dung bang phan co so — bo
     * benchmark Augerat chay y nhu truoc.
     *
     * BAC UU TIEN phai LON HON MOI chenh lech quang duong giua hai phuong an bat ky. Vi du
     * het cho cho mot trong hai don: don A uu tien cao nhung o xa (+20 km), don B o gan
     * (+2 km). Bo B thi tong tang 18 km, nhung tien phat chenh nhau it nhat mot bac — bac
     * lon hon 18 km thi bo giai van chon giu A. Bac nho hon thi so km thang, A bi bo.
     *
     * Chan tren cua chenh lech, de bac luon thang:
     *   - quang duong: moi tuyen di qua toi da (so diem + so xe) canh, moi canh khong dai
     *     hon o lon nhat
     *   - so diem bi bo co the khac nhau giua hai phuong an, moi diem lech toi da mot
     *     phan co so
     */
    private long[] dropPenaltiesFor(List<OptimizationPoint> deliveryPoints,
                                    double[][] matrix,
                                    int vehicleCount) {
        double longestLeg = 0;
        for (double[] row : matrix) {
            for (double cell : row) {
                if (cell > longestLeg) {
                    longestLeg = cell;
                }
            }
        }
        // Ma tran toan 0 (moi diem trung toa do) van phai cho ra tien phat duong
        long basePenalty = Math.max(1, Math.round(longestLeg * 10));

        int pointCount = deliveryPoints.size();
        long priorityStep = Math.addExact(
                Math.multiplyExact(pointCount + vehicleCount + 1L, Math.round(longestLeg) + 1),
                Math.multiplyExact((long) pointCount, basePenalty));

        // multiplyExact / addExact: lo tran so long thi nem loi ngay, khong am tham ra so am
        long[] penalties = new long[pointCount];
        for (int i = 0; i < pointCount; i++) {
            long priority = deliveryPoints.get(i).getDropPriority();
            penalties[i] = Math.addExact(basePenalty, Math.multiplyExact(priorityStep, priority));
        }
        return penalties;
    }

    @Override
    public AlgorithmType algorithmType() {
        return AlgorithmType.OR_TOOLS;
    }
}
