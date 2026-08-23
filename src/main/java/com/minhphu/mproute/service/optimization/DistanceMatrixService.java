package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.entity.DistanceMatrixEntry;
import com.minhphu.mproute.entity.enums.DistanceSource;
import com.minhphu.mproute.repository.DistanceMatrixEntryRepository;
import com.minhphu.mproute.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Nap ma tran khoang cach vao BO NHO mot lan truoc khi thuat toan chay (CN-03-05).
 *
 * DAY LA LOP QUAN TRONG NHAT VE HIEU NANG CUA CA DO AN. Ly do:
 * 2-opt hoi khoang cach hang nghin lan cho mot tuyen. Neu moi lan hoi la mot request
 * HTTP toi OSRM hoac mot cau SELECT xuong MySQL thi chuong trinh treo — day chinh la
 * dieu cam so 4 trong quy uoc ky thuat. Sau khi goi loadMatrix() xong, thuat toan chi
 * con tra matrix[i][j] tren mang hai chieu, khong dung toi mang lan CSDL nua.
 *
 * Bon buoc dung nhu dac ta docs/03-thuat-toan.md muc 4:
 *   B1 — tra bo nho dem trong bang distance_matrix theo toa do (CBR-12)
 *   B2 — con o nao thieu thi goi OSRM Table API MOT lan cho toan bo diem,
 *        OSRM chet thi lui ve Haversine va bat co approximate (CBR-11, MSG-09)
 *   B3 — DOI XUNG HOA ma tran, bat buoc, neu khong 2-opt tinh sai
 *   B4 — tra ve
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistanceMatrixService {

    /** Chi so 0 trong ma tran LUON la kho. Quy uoc xuyen suot do an. */
    public static final int DEPOT_INDEX = 0;

    /*
     * Cot toa do trong CSDL la DECIMAL(10,7). Lam tron ve dung 7 chu so thap phan
     * truoc khi dung lam khoa, neu khong thi 21.00670000000001 va 21.0067 thanh hai
     * khoa khac nhau, tra bo nho dem khong bao gio trung.
     */
    private static final int COORDINATE_SCALE = 7;

    private final DistanceMatrixEntryRepository distanceMatrixEntryRepository;
    private final SystemParameterService systemParameterService;

    /**
     * Ket qua mot lan nap ma tran.
     *
     * @param meters       ma tran khoang cach da doi xung hoa, don vi met
     * @param approximate  true khi co it nhat mot o phai dung Haversine (MSG-09)
     * @param cacheHits    de chung minh bo nho dem co tac dung — chay lan hai voi
     *                     cung tap diem thi con so nay phai bang n*n - n
     * @param cacheMisses  so o phai lay moi trong lan chay nay
     */
    public record DistanceMatrix(double[][] meters,
                                 boolean approximate,
                                 int cacheHits,
                                 int cacheMisses) {

        /** Nguon de ghi vao bang ket qua va hien ra man hinh. */
        public DistanceSource source() {
            return approximate ? DistanceSource.HAVERSINE : DistanceSource.OSRM;
        }
    }

    /**
     * Nap ma tran cho mot tap diem. Diem dau tien PHAI la kho.
     *
     * Ham nay dat luon matrixIndex cho tung diem theo dung thu tu trong danh sach,
     * de goi ben ngoai khong phai tu danh so va khong the danh lech.
     */
    @Transactional
    public DistanceMatrix loadMatrix(List<OptimizationPoint> points) {
        int size = points.size();
        double[][] matrix = new double[size][size];

        for (int i = 0; i < size; i++) {
            points.get(i).setMatrixIndex(i);
        }
        if (size > 0 && !points.get(DEPOT_INDEX).isDepot()) {
            log.warn("Diem o chi so 0 khong phai kho ({}). Quy uoc cua do an la kho luon dung dau.",
                    points.get(DEPOT_INDEX).getName());
        }

        // ---------- B1: tra bo nho dem (CBR-12) ----------
        Map<String, Double> cache = loadCache(points);
        List<int[]> missingCells = new ArrayList<>();
        int cacheHits = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    matrix[i][j] = 0;      // di tu mot diem toi chinh no
                    continue;
                }
                Double cached = cache.get(cacheKeyFor(points.get(i), points.get(j)));
                if (cached != null) {
                    matrix[i][j] = cached;
                    cacheHits++;
                } else {
                    matrix[i][j] = Double.NaN;   // danh dau la con thieu
                    missingCells.add(new int[]{i, j});
                }
            }
        }

        // ---------- B2: con thieu thi goi OSRM mot lan ----------
        boolean approximate = false;
        int cacheMisses = missingCells.size();

        if (!missingCells.isEmpty()) {
            approximate = fillMissingCells(points, matrix, missingCells);
        }

        log.info("Ma tran {}x{}: {} o lay tu bo nho dem, {} o phai lay moi, nguon {}",
                size, size, cacheHits, cacheMisses, approximate ? "Haversine (xap xi)" : "OSRM");

        // ---------- B3: doi xung hoa (BAT BUOC) ----------
        symmetrize(matrix);

        // ---------- B4 ----------
        return new DistanceMatrix(matrix, approximate, cacheHits, cacheMisses);
    }

    // ==================== B1 ====================

    /**
     * Nap ca bo nho dem lien quan bang MOT cau truy van roi do vao Map.
     *
     * Khong tra tung cap mot: ma tran 16 diem co 240 cap, tra tung cap la 240 cau
     * SELECT. Mot cau lay ve roi tra trong bo nho la 240 lan tra Map.
     *
     * Cau truy van loc theo toa do DIEM DAU nen co the lay ve du vai dong khong dung
     * toi — vo hai, vi buoc sau tra theo dung khoa gom ca bon toa do.
     */
    private Map<String, Double> loadCache(List<OptimizationPoint> points) {
        List<Double> latitudes = points.stream().map(p -> roundCoordinate(p.getLatitude())).toList();
        List<Double> longitudes = points.stream().map(p -> roundCoordinate(p.getLongitude())).toList();

        List<DistanceMatrixEntry> rows =
                distanceMatrixEntryRepository.findByFromLatitudeInAndFromLongitudeIn(latitudes, longitudes);

        Map<String, Double> cache = new HashMap<>();
        for (DistanceMatrixEntry row : rows) {
            cache.put(cacheKey(row.getFromLatitude(), row.getFromLongitude(),
                               row.getToLatitude(), row.getToLongitude()),
                    row.getDistanceMeters());
        }
        return cache;
    }

    // ==================== B2 ====================

    /**
     * Lay cac o con thieu. Tra ve true neu phai dung Haversine (ma tran la xap xi).
     *
     * Goi OSRM MOT lan cho TOAN BO diem chu khong phai chi cac cap con thieu:
     * Table API tinh ca ma tran trong mot lan, chia nho ra goi nhieu lan chi cham hon.
     */
    private boolean fillMissingCells(List<OptimizationPoint> points,
                                     double[][] matrix,
                                     List<int[]> missingCells) {

        Optional<OsrmDistanceCalculator.DistanceTable> osrmTable = callOsrm(points);

        HaversineDistanceCalculator haversine = new HaversineDistanceCalculator(
                systemParameterService.getDouble(SystemParameterService.HAVERSINE_FACTOR));

        if (osrmTable.isEmpty()) {
            // CBR-11 + MSG-09: OSRM chet thi van phai ra ket qua, chi la xap xi
            log.warn("MSG-09: Khong ket noi duoc OSRM. Dang dung khoang cach xap xi "
                    + "(Haversine x {}).", haversine.getDetourFactor());

            for (int[] cell : missingCells) {
                matrix[cell[0]][cell[1]] =
                        haversine.distanceBetween(points.get(cell[0]), points.get(cell[1]));
            }

            /*
             * CO Y KHONG luu gia tri Haversine vao bang distance_matrix.
             * Bang do la bo nho dem CUA OSRM. Luu so xap xi vao thi lan sau tra thay
             * "da co" roi khong bao gio goi OSRM nua — ma tran vinh vien xap xi ma
             * khong ai biet, vi luc do co approximate cung khong con bat len.
             */
            return true;
        }

        double[][] osrmMeters = osrmTable.get().meters();
        int[][] osrmSeconds = osrmTable.get().seconds();
        List<DistanceMatrixEntry> toSave = new ArrayList<>();
        boolean anyApproximate = false;

        for (int[] cell : missingCells) {
            int i = cell[0];
            int j = cell[1];
            double meters = osrmMeters[i][j];

            if (Double.isNaN(meters)) {
                /*
                 * OSRM khong tim duoc duong di giua hai diem nay: thieu du lieu ban do,
                 * hoac toa do roi vao cho khong co duong. Lap bang Haversine cho rieng
                 * o do va KHONG luu vao bo nho dem, vi day khong phai so cua OSRM.
                 */
                matrix[i][j] = haversine.distanceBetween(points.get(i), points.get(j));
                anyApproximate = true;
                continue;
            }

            matrix[i][j] = meters;
            toSave.add(newCacheEntry(points.get(i), points.get(j), meters, osrmSeconds[i][j]));
        }

        if (!toSave.isEmpty()) {
            distanceMatrixEntryRepository.saveAll(toSave);
            log.info("Da luu {} o moi vao bo nho dem distance_matrix", toSave.size());
        }
        return anyApproximate;
    }

    /** Tao bo tinh OSRM theo tham so he thong, de doi dia chi khong phai bien dich lai. */
    private Optional<OsrmDistanceCalculator.DistanceTable> callOsrm(List<OptimizationPoint> points) {
        String serverUrl = systemParameterService.getValue(SystemParameterService.OSRM_URL);
        int timeoutSeconds = systemParameterService.getInt(SystemParameterService.OSRM_TIMEOUT);
        return new OsrmDistanceCalculator(serverUrl, timeoutSeconds).fetchDistanceTable(points);
    }

    private DistanceMatrixEntry newCacheEntry(OptimizationPoint from, OptimizationPoint to,
                                              double meters, int seconds) {
        DistanceMatrixEntry entry = new DistanceMatrixEntry();
        entry.setFromLatitude(roundCoordinate(from.getLatitude()));
        entry.setFromLongitude(roundCoordinate(from.getLongitude()));
        entry.setToLatitude(roundCoordinate(to.getLatitude()));
        entry.setToLongitude(roundCoordinate(to.getLongitude()));
        entry.setDistanceMeters(meters);
        entry.setDurationSeconds(seconds);
        entry.setSource(DistanceSource.OSRM);
        return entry;
    }

    // ==================== B3 ====================

    /**
     * DOI XUNG HOA — buoc bat buoc, bo la 2-opt tinh sai.
     *
     * OSRM tra ve ma tran BAT DOI XUNG: D[i][j] khac D[j][i] vi duong mot chieu,
     * cam re trai, dai phan cach. Nhung cong thuc bon canh cua 2-opt
     *
     *     delta = D[a][c] + D[b][d] - D[a][b] - D[c][d]
     *
     * chi dung khi D[x][y] = D[y][x]. De nguyen ma tran tho thi 2-opt sai theo kieu
     * te nhat: no TUONG la cai thien roi nhan phep dao, trong khi tuyen that dai ra.
     * Da kiem chung bang so trong docs/03: delta = -9 nhung tuyen dai them 19.
     *
     * Lay trung binh cong lam gia dinh "khoang cach doi xung" thanh dung theo cau tao.
     * Cai gia phai tra la mat mot phan thong tin ve duong mot chieu — da ghi vao
     * muc han che 4.7 cua bao cao.
     */
    private void symmetrize(double[][] matrix) {
        int size = matrix.length;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                double average = (matrix[i][j] + matrix[j][i]) / 2.0;
                matrix[i][j] = average;
                matrix[j][i] = average;
            }
        }
    }

    // ==================== Ham dung chung ====================

    private String cacheKeyFor(OptimizationPoint from, OptimizationPoint to) {
        return cacheKey(from.getLatitude(), from.getLongitude(),
                        to.getLatitude(), to.getLongitude());
    }

    /**
     * Khoa tra bo nho dem: bon toa do lam tron 7 chu so thap phan.
     *
     * Locale.ROOT bat buoc — may cai dat tieng Viet dinh dang so thap phan bang dau
     * phay, ma dau phay lai la ky tu ngan cach trong chuoi khoa nay.
     */
    private String cacheKey(double fromLat, double fromLon, double toLat, double toLon) {
        return String.format(Locale.ROOT, "%.7f,%.7f>%.7f,%.7f",
                roundCoordinate(fromLat), roundCoordinate(fromLon),
                roundCoordinate(toLat), roundCoordinate(toLon));
    }

    private double roundCoordinate(double value) {
        return BigDecimal.valueOf(value)
                .setScale(COORDINATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
