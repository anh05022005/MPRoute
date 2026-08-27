package com.minhphu.mproute.service.optimization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Doc file .vrp cua CVRPLIB (CN-09-02).
 *
 * Dinh dang file — vi du A-n32-k5.vrp:
 *
 *     NAME : A-n32-k5
 *     COMMENT : (Augerat et al, No of trucks: 5, Optimal value: 784)
 *     TYPE : CVRP
 *     DIMENSION : 32
 *     EDGE_WEIGHT_TYPE : EUC_2D
 *     CAPACITY : 100
 *     NODE_COORD_SECTION
 *      1 82 76            <- so hieu diem, x, y
 *      ...
 *     DEMAND_SECTION
 *     1 0                 <- so hieu diem, nhu cau. Kho luon bang 0
 *     ...
 *     DEPOT_SECTION
 *      1
 *      -1
 *     EOF
 *
 * Lop nay khong dung Spring, khong dung CSDL — chay duoc tu main() cua BenchmarkRunner.
 *
 * KHONG DUNG THU VIEN DOC .vrp CO SAN: dinh dang chi co bay tu khoa, tu doc mat khoang
 * mot tram dong, ma doi lai la khi hoi dong hoi "em doc file chuan the nao" thi mo thang
 * file nay ra chi duoc. Them mot thu vien nua chi de doc bay dong tieu de la khong dang.
 */
public final class VrpFileReader {

    /** Thu muc chua bo du lieu trong resources, nap qua classpath chu khong qua duong dan o dia. */
    public static final String AUGERAT_FOLDER = "/data/augerat/";

    /*
     * Doc so xe va loi giai toi uu tu dong COMMENT. Ca hai deu nam san trong file nen
     * KHONG CHEP TAY vao code: chep tay la co nguy co chep nham mot chu so roi ca bang
     * gap sai theo ma khong ai phat hien ra.
     */
    private static final Pattern TRUCK_COUNT = Pattern.compile("No of trucks\\s*:\\s*(\\d+)");
    private static final Pattern OPTIMAL_VALUE = Pattern.compile("Optimal value\\s*:\\s*(\\d+)");

    private VrpFileReader() {
    }

    /** Doc mot bai theo ten, vi du readInstance("A-n32-k5"). */
    public static VrpInstance readInstance(String instanceName) {
        return readResource(AUGERAT_FOLDER + instanceName + ".vrp");
    }

    /** Doc mot bai tu duong dan classpath, vi du "/data/augerat/A-n32-k5.vrp". */
    public static VrpInstance readResource(String resourcePath) {
        try (InputStream input = VrpFileReader.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException(
                        "Khong tim thay file du lieu chuan: " + resourcePath
                                + ". Tai tu CVRPLIB va dat vao src/main/resources" + AUGERAT_FOLDER);
            }
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return parse(lines);
        } catch (IOException failedToRead) {
            throw new UncheckedIOException("Loi doc file " + resourcePath, failedToRead);
        }
    }

    /**
     * Phan tich noi dung file thanh mot VrpInstance.
     *
     * Doc theo mot vong duy nhat voi mot bien "dang o phan nao", vi cac phan cua file
     * luon di theo dung thu tu nay va khong long nhau.
     */
    static VrpInstance parse(List<String> lines) {

        String name = "";
        int dimension = 0;
        double capacity = 0;
        int vehicleCount = 0;
        double optimalValue = 0;
        String edgeWeightType = "";

        // Chi so trong mang = SO HIEU DIEM cua file (bat dau tu 1), nen cap phat du mot phan tu
        double[] x = new double[1];
        double[] y = new double[1];
        double[] demand = new double[1];
        int depotNodeId = 1;

        String section = "";

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("EOF")) {
                break;
            }

            // Cac dong tieu de deu co dang "TU KHOA : gia tri"
            if (line.contains(":")) {
                String key = line.substring(0, line.indexOf(':')).trim().toUpperCase(Locale.ROOT);
                String value = line.substring(line.indexOf(':') + 1).trim();
                switch (key) {
                    case "NAME" -> name = value;
                    case "DIMENSION" -> dimension = Integer.parseInt(value);
                    case "CAPACITY" -> capacity = Double.parseDouble(value);
                    case "EDGE_WEIGHT_TYPE" -> edgeWeightType = value;
                    case "COMMENT" -> {
                        Matcher trucks = TRUCK_COUNT.matcher(value);
                        if (trucks.find()) {
                            vehicleCount = Integer.parseInt(trucks.group(1));
                        }
                        Matcher optimal = OPTIMAL_VALUE.matcher(value);
                        if (optimal.find()) {
                            optimalValue = Double.parseDouble(optimal.group(1));
                        }
                    }
                    default -> {
                        // TYPE va cac tu khoa khac khong dung toi
                    }
                }
                continue;
            }

            if (line.endsWith("_SECTION")) {
                section = line;
                if (dimension > 0 && x.length == 1) {
                    x = new double[dimension + 1];
                    y = new double[dimension + 1];
                    demand = new double[dimension + 1];
                }
                continue;
            }

            String[] parts = line.split("\\s+");
            switch (section) {
                case "NODE_COORD_SECTION" -> {
                    int nodeId = Integer.parseInt(parts[0]);
                    x[nodeId] = Double.parseDouble(parts[1]);
                    y[nodeId] = Double.parseDouble(parts[2]);
                }
                case "DEMAND_SECTION" -> {
                    int nodeId = Integer.parseInt(parts[0]);
                    demand[nodeId] = Double.parseDouble(parts[1]);
                }
                case "DEPOT_SECTION" -> {
                    // Phan nay ket thuc bang -1; so hieu dau tien la kho
                    int nodeId = Integer.parseInt(parts[0]);
                    if (nodeId > 0) {
                        depotNodeId = nodeId;
                    }
                }
                default -> {
                    // Ngoai ba phan tren khong con phan nao can doc
                }
            }
        }

        if (!edgeWeightType.startsWith("EUC_2D")) {
            throw new IllegalArgumentException("Bai " + name + " dung EDGE_WEIGHT_TYPE = "
                    + edgeWeightType + ", chuong trinh chi doc duoc EUC_2D");
        }

        /*
         * Ca bo Augerat deu dat kho o diem so 1, va toan bo do an quy uoc kho nam o chi so
         * 0 cua ma tran. Gap file dat kho o cho khac thi DUNG LAI VA BAO RO, khong am tham
         * chay tiep — chay tiep se ra mot con so gap sai ma nhin vao khong biet vi sao.
         */
        if (depotNodeId != 1) {
            throw new IllegalArgumentException("Bai " + name + " dat kho o diem so " + depotNodeId
                    + ", chuong trinh chi doc duoc file co kho o diem so 1");
        }

        List<OptimizationPoint> points = buildPoints(dimension, x, y, demand);
        return new VrpInstance(name, dimension, capacity, vehicleCount, optimalValue,
                points, buildEuclideanMatrix(points));
    }

    /**
     * Doi cac diem cua file thanh OptimizationPoint cua do an.
     *
     * Diem so 1 cua file thanh kho o chi so 0, cac diem con lai giu nguyen thu tu.
     * The tich de bang 0 cho moi diem — xem giai thich o VrpInstance.
     */
    private static List<OptimizationPoint> buildPoints(int dimension, double[] x, double[] y,
                                                       double[] demand) {
        List<OptimizationPoint> points = new ArrayList<>();

        /*
         * Toa do cua bo chuan la toa do mat phang, khong phai vi do / kinh do. Van dat
         * vao hai truong latitude / longitude vi ma tran o duoi tinh bang cong thuc Euclid
         * tren chinh hai so do, khong goi toi Haversine hay OSRM. Neu sau nay co ai do
         * ve bo du lieu nay len ban do Leaflet thi se ra mot dam diem vo nghia — dung, va
         * do la ly do lop nay chi phuc vu benchmark chu khong dung o duong chay that.
         */
        OptimizationPoint depot = OptimizationPoint.depot(y[1], x[1]);
        depot.setMatrixIndex(0);
        points.add(depot);

        for (int nodeId = 2; nodeId <= dimension; nodeId++) {
            OptimizationPoint point = OptimizationPoint.forOrder(
                    (long) nodeId, "Diem " + nodeId, y[nodeId], x[nodeId], demand[nodeId], 0.0);
            point.setMatrixIndex(nodeId - 1);
            points.add(point);
        }
        return points;
    }

    /**
     * Ma tran khoang cach theo quy uoc EUC_2D cua CVRPLIB:
     *
     *     d(i,j) = lam_tron( can_bac_hai( (xi-xj)^2 + (yi-yj)^2 ) )
     *
     * LAM TRON VE SO NGUYEN GAN NHAT LA BAT BUOC, khong phai de cho dep. Loi giai toi uu
     * 784 cua A-n32-k5 duoc cong bo theo dung quy uoc nay; giu nguyen so thuc thi ket qua
     * lech vai don vi va con so gap cua ca bang sai theo. Day chinh la loi hay gap nhat
     * khi chay benchmark lan dau — gap ra am hoac thap bat thuong thi kiem lai dong nay
     * truoc tien.
     *
     * Ma tran doi xung san (d(i,j) = d(j,i)) vi Euclid doi xung, nen 2-opt dung duoc ngay
     * khong can buoc doi xung hoa nhu voi ma tran OSRM.
     */
    private static double[][] buildEuclideanMatrix(List<OptimizationPoint> points) {
        int size = points.size();
        double[][] matrix = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    continue;
                }
                // latitude giu y, longitude giu x — xem comment o buildPoints
                double deltaX = points.get(i).getLongitude() - points.get(j).getLongitude();
                double deltaY = points.get(i).getLatitude() - points.get(j).getLatitude();
                matrix[i][j] = Math.round(Math.sqrt(deltaX * deltaX + deltaY * deltaY));
            }
        }
        return matrix;
    }
}
