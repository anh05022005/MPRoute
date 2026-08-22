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
 * dieu cam so 4 trong quy uoc ky thuat. Sau khi goi napMaTran() xong, thuat toan chi
 * con tra matrix[i][j] tren mang hai chieu, khong dung toi mang lan CSDL nua.
 *
 * Bon buoc dung nhu dac ta docs/03-thuat-toan.md muc 4:
 *   B1 — tra bo nho dem trong bang distance_matrix theo toa do (CBR-12)
 *   B2 — con o nao thieu thi goi OSRM Table API MOT lan cho toan bo diem,
 *        OSRM chet thi lui ve Haversine va bat co dungXapXi (CBR-11, MSG-09)
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
    private static final int SO_CHU_SO_TOA_DO = 7;

    private final DistanceMatrixEntryRepository distanceMatrixEntryRepository;
    private final SystemParameterService systemParameterService;

    /**
     * Ket qua mot lan nap ma tran.
     *
     * @param met            ma tran khoang cach da doi xung hoa, don vi met
     * @param dungXapXi      true khi co it nhat mot o phai dung Haversine (MSG-09)
     * @param soOLayTuCache  de chung minh bo nho dem co tac dung — chay lan hai
     *                       voi cung tap diem thi con so nay phai bang n*n - n
     * @param soOGoiMoi      so o phai lay moi trong lan chay nay
     */
    public record MaTranKhoangCach(double[][] met,
                                   boolean dungXapXi,
                                   int soOLayTuCache,
                                   int soOGoiMoi) {

        /** Nguon de ghi vao bang ket qua va hien ra man hinh. */
        public DistanceSource nguon() {
            return dungXapXi ? DistanceSource.HAVERSINE : DistanceSource.OSRM;
        }
    }

    /**
     * Nap ma tran cho mot tap diem. Diem dau tien PHAI la kho.
     *
     * Ham nay dat luon matrixIndex cho tung diem theo dung thu tu trong danh sach,
     * de goi ben ngoai khong phai tu danh so va khong the danh lech.
     */
    @Transactional
    public MaTranKhoangCach napMaTran(List<OptimizationPoint> cacDiem) {
        int n = cacDiem.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            cacDiem.get(i).setMatrixIndex(i);
        }
        if (n > 0 && !cacDiem.get(DEPOT_INDEX).isDepot()) {
            log.warn("Diem o chi so 0 khong phai kho ({}). Quy uoc cua do an la kho luon dung dau.",
                    cacDiem.get(DEPOT_INDEX).getName());
        }

        // ---------- B1: tra bo nho dem (CBR-12) ----------
        Map<String, Double> boNhoDem = napBoNhoDem(cacDiem);
        List<int[]> cacOConThieu = new ArrayList<>();
        int soOLayTuCache = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;      // di tu mot diem toi chinh no
                    continue;
                }
                Double daCo = boNhoDem.get(khoaCap(cacDiem.get(i), cacDiem.get(j)));
                if (daCo != null) {
                    matrix[i][j] = daCo;
                    soOLayTuCache++;
                } else {
                    matrix[i][j] = Double.NaN;   // danh dau la con thieu
                    cacOConThieu.add(new int[]{i, j});
                }
            }
        }

        // ---------- B2: con thieu thi goi OSRM mot lan ----------
        boolean dungXapXi = false;
        int soOGoiMoi = cacOConThieu.size();

        if (!cacOConThieu.isEmpty()) {
            dungXapXi = layPhanConThieu(cacDiem, matrix, cacOConThieu);
        }

        log.info("Ma tran {}x{}: {} o lay tu bo nho dem, {} o phai lay moi, nguon {}",
                n, n, soOLayTuCache, soOGoiMoi, dungXapXi ? "Haversine (xap xi)" : "OSRM");

        // ---------- B3: doi xung hoa (BAT BUOC) ----------
        doiXungHoa(matrix);

        // ---------- B4 ----------
        return new MaTranKhoangCach(matrix, dungXapXi, soOLayTuCache, soOGoiMoi);
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
    private Map<String, Double> napBoNhoDem(List<OptimizationPoint> cacDiem) {
        List<Double> cacViDo = cacDiem.stream().map(d -> lamTronToaDo(d.getLatitude())).toList();
        List<Double> cacKinhDo = cacDiem.stream().map(d -> lamTronToaDo(d.getLongitude())).toList();

        List<DistanceMatrixEntry> cacDong =
                distanceMatrixEntryRepository.findByFromLatitudeInAndFromLongitudeIn(cacViDo, cacKinhDo);

        Map<String, Double> boNhoDem = new HashMap<>();
        for (DistanceMatrixEntry dong : cacDong) {
            boNhoDem.put(khoa(dong.getFromLatitude(), dong.getFromLongitude(),
                              dong.getToLatitude(), dong.getToLongitude()),
                    dong.getDistanceMeters());
        }
        return boNhoDem;
    }

    // ==================== B2 ====================

    /**
     * Lay cac o con thieu. Tra ve true neu phai dung Haversine (ma tran la xap xi).
     *
     * Goi OSRM MOT lan cho TOAN BO diem chu khong phai chi cac cap con thieu:
     * Table API tinh ca ma tran trong mot lan, chia nho ra goi nhieu lan chi cham hon.
     */
    private boolean layPhanConThieu(List<OptimizationPoint> cacDiem,
                                    double[][] matrix,
                                    List<int[]> cacOConThieu) {

        Optional<OsrmDistanceCalculator.BangKhoangCach> tuOsrm = goiOsrm(cacDiem);

        HaversineDistanceCalculator haversine = new HaversineDistanceCalculator(
                systemParameterService.getDouble(SystemParameterService.HAVERSINE_FACTOR));

        if (tuOsrm.isEmpty()) {
            // CBR-11 + MSG-09: OSRM chet thi van phai ra ket qua, chi la xap xi
            log.warn("MSG-09: Khong ket noi duoc OSRM. Dang dung khoang cach xap xi "
                    + "(Haversine x {}).", haversine.getHeSoQuyDoi());

            for (int[] o : cacOConThieu) {
                matrix[o[0]][o[1]] = haversine.khoangCach(cacDiem.get(o[0]), cacDiem.get(o[1]));
            }

            /*
             * CO Y KHONG luu gia tri Haversine vao bang distance_matrix.
             * Bang do la bo nho dem CUA OSRM. Luu so xap xi vao thi lan sau tra thay
             * "da co" roi khong bao gio goi OSRM nua — ma tran vinh vien xap xi ma
             * khong ai biet, vi luc do co dungXapXi cung khong con bat len.
             */
            return true;
        }

        double[][] osrm = tuOsrm.get().met();
        int[][] giay = tuOsrm.get().giay();
        List<DistanceMatrixEntry> canLuu = new ArrayList<>();
        boolean coONaoXapXi = false;

        for (int[] o : cacOConThieu) {
            int i = o[0];
            int j = o[1];
            double met = osrm[i][j];

            if (Double.isNaN(met)) {
                /*
                 * OSRM khong tim duoc duong di giua hai diem nay: thieu du lieu ban do,
                 * hoac toa do roi vao cho khong co duong. Lap bang Haversine cho rieng
                 * o do va KHONG luu vao bo nho dem, vi day khong phai so cua OSRM.
                 */
                matrix[i][j] = haversine.khoangCach(cacDiem.get(i), cacDiem.get(j));
                coONaoXapXi = true;
                continue;
            }

            matrix[i][j] = met;
            canLuu.add(dungBanGhi(cacDiem.get(i), cacDiem.get(j), met, giay[i][j]));
        }

        if (!canLuu.isEmpty()) {
            distanceMatrixEntryRepository.saveAll(canLuu);
            log.info("Da luu {} o moi vao bo nho dem distance_matrix", canLuu.size());
        }
        return coONaoXapXi;
    }

    /** Tao bo tinh OSRM theo tham so he thong, de doi dia chi khong phai bien dich lai. */
    private Optional<OsrmDistanceCalculator.BangKhoangCach> goiOsrm(List<OptimizationPoint> cacDiem) {
        String diaChi = systemParameterService.getValue(SystemParameterService.OSRM_URL);
        int timeoutGiay = systemParameterService.getInt(SystemParameterService.OSRM_TIMEOUT);
        return new OsrmDistanceCalculator(diaChi, timeoutGiay).bangKhoangCach(cacDiem);
    }

    private DistanceMatrixEntry dungBanGhi(OptimizationPoint tu, OptimizationPoint toi,
                                           double met, int giay) {
        DistanceMatrixEntry ban = new DistanceMatrixEntry();
        ban.setFromLatitude(lamTronToaDo(tu.getLatitude()));
        ban.setFromLongitude(lamTronToaDo(tu.getLongitude()));
        ban.setToLatitude(lamTronToaDo(toi.getLatitude()));
        ban.setToLongitude(lamTronToaDo(toi.getLongitude()));
        ban.setDistanceMeters(met);
        ban.setDurationSeconds(giay);
        ban.setSource(DistanceSource.OSRM);
        return ban;
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
    private void doiXungHoa(double[][] matrix) {
        int n = matrix.length;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double trungBinh = (matrix[i][j] + matrix[j][i]) / 2.0;
                matrix[i][j] = trungBinh;
                matrix[j][i] = trungBinh;
            }
        }
    }

    // ==================== Ham dung chung ====================

    private String khoaCap(OptimizationPoint tu, OptimizationPoint toi) {
        return khoa(tu.getLatitude(), tu.getLongitude(), toi.getLatitude(), toi.getLongitude());
    }

    /**
     * Khoa tra bo nho dem: bon toa do lam tron 7 chu so thap phan.
     *
     * Locale.ROOT bat buoc — may cai dat tieng Viet dinh dang so thap phan bang dau
     * phay, ma dau phay lai la ky tu ngan cach trong chuoi khoa nay.
     */
    private String khoa(double tuVi, double tuKinh, double toiVi, double toiKinh) {
        return String.format(Locale.ROOT, "%.7f,%.7f>%.7f,%.7f",
                lamTronToaDo(tuVi), lamTronToaDo(tuKinh),
                lamTronToaDo(toiVi), lamTronToaDo(toiKinh));
    }

    private double lamTronToaDo(double giaTri) {
        return BigDecimal.valueOf(giaTri)
                .setScale(SO_CHU_SO_TOA_DO, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
