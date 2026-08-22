package com.minhphu.mproute.service.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Khoang cach duong di THAT, lay tu may chu OSRM chay bang Docker tren chinh may nay.
 *
 * Dung Table API de lay CA MA TRAN trong MOT lan goi:
 *
 *     GET /table/v1/driving/{lon1},{lat1};{lon2},{lat2};...?annotations=distance,duration
 *
 * Vi sao khong goi tung cap mot: 16 diem la 16 x 16 = 256 cap, goi tung cap la 256
 * request HTTP. Table API tra ve ca 256 o trong mot lan.
 *
 * CHU Y THU TU TOA DO: OSRM nhan {kinh do},{vi do} — nguoc voi thoi quen doc
 * "vi do, kinh do" cua nguoi Viet va nguoc voi Leaflet. Dat nham thu tu thi khong
 * bao loi, chi ra khoang cach sai (toa do roi xuong bien Somalia).
 */
@Slf4j
public class OsrmDistanceCalculator implements DistanceCalculator {

    /*
     * OSRM mac dinh chan Table API o 100 diem moi lan goi (tham so max-table-size).
     * Quy mo do an la 15-30 diem nen khong cham nguong. Vuot nguong thi ham nay tra
     * ve rong de goi ben ngoai lui ve Haversine, chu khong de OSRM tra loi kho hieu.
     */
    private static final int SO_DIEM_TOI_DA = 100;

    private final String diaChiMayChu;
    private final RestClient restClient;

    public OsrmDistanceCalculator(String diaChiMayChu, int timeoutGiay) {
        this.diaChiMayChu = diaChiMayChu;

        /*
         * Dat timeout la BAT BUOC. Mac dinh cua Java la cho vo han: OSRM treo giua
         * chung thi man hinh phan tuyen quay mai khong bao gio bao loi, ma dung
         * phuong an du phong Haversine thi da xong tu lau.
         */
        SimpleClientHttpRequestFactory nhaMay = new SimpleClientHttpRequestFactory();
        nhaMay.setConnectTimeout(Duration.ofSeconds(timeoutGiay));
        nhaMay.setReadTimeout(Duration.ofSeconds(timeoutGiay));

        this.restClient = RestClient.builder()
                .baseUrl(diaChiMayChu)
                .requestFactory(nhaMay)
                .build();
    }

    /** Ma tran khoang cach (met) va thoi gian di chuyen (giay) cua cung mot tap diem. */
    public record BangKhoangCach(double[][] met, int[][] giay) {}

    /**
     * Lay ca ma tran trong mot lan goi.
     *
     * @return rong khi OSRM khong chay, tra loi, hoac vuot gioi han so diem —
     *         goi ben ngoai se lui ve Haversine (CBR-11). Day KHONG phai loi
     *         chuong trinh, la truong hop da luong truoc.
     */
    public Optional<BangKhoangCach> bangKhoangCach(List<OptimizationPoint> cacDiem) {
        if (cacDiem == null || cacDiem.isEmpty()) {
            return Optional.empty();
        }
        if (cacDiem.size() > SO_DIEM_TOI_DA) {
            log.warn("Co {} diem, vuot gioi han {} cua OSRM Table API. Dung Haversine.",
                    cacDiem.size(), SO_DIEM_TOI_DA);
            return Optional.empty();
        }

        String duongDan = "/table/v1/driving/" + chuoiToaDo(cacDiem)
                + "?annotations=distance,duration";

        try {
            TableResponse traLoi = restClient.get()
                    .uri(duongDan)
                    .retrieve()
                    .body(TableResponse.class);

            if (traLoi == null || !"Ok".equals(traLoi.code()) || traLoi.distances() == null) {
                log.warn("OSRM tra ve khong hop le: {}", traLoi == null ? "null" : traLoi.code());
                return Optional.empty();
            }

            return Optional.of(doiSangMang(traLoi, cacDiem.size()));

        } catch (Exception loi) {
            /*
             * Nuot loi CO CHU DICH. OSRM khong chay la truong hop binh thuong tren may
             * chua dung Docker, va CBR-11 da co phuong an du phong. Ghi log de con biet
             * ma tran nay la xap xi chu khong phai duong di that.
             */
            log.warn("Khong goi duoc OSRM tai {}: {}. Se dung Haversine.",
                    diaChiMayChu, loi.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Khoang cach mot cap diem. Chi dung cho viec le, vi du kiem thu ket noi.
     *
     * KHONG dung ham nay trong vong lap thuat toan — moi lan goi la mot request HTTP.
     * Ham nay tra ve 0 khi OSRM loi; cho can du phong that thi dung
     * DistanceMatrixService, o do co Haversine dam nhiem.
     */
    @Override
    public double khoangCach(OptimizationPoint diemA, OptimizationPoint diemB) {
        return bangKhoangCach(List.of(diemA, diemB))
                .map(bang -> bang.met()[0][1])
                .orElse(0.0);
    }

    @Override
    public String sourceName() {
        return "OSRM";
    }

    /** Kiem OSRM co dang chay khong, dung de hien trang thai tren man hinh phan tuyen. */
    public boolean dangChay() {
        OptimizationPoint kho = OptimizationPoint.depot(20.9497, 105.8172);
        return bangKhoangCach(List.of(kho, kho)).isPresent();
    }

    // ==================== Ham rieng ====================

    /**
     * Ghep danh sach diem thanh chuoi "lon,lat;lon,lat;..." theo dung khuon OSRM.
     *
     * Locale.ROOT la BAT BUOC: may cai dat tieng Viet dinh dang so thap phan bang
     * DAU PHAY, ma dau phay lai chinh la ky tu ngan cach lon voi lat. Khong dat
     * locale thi chuoi gui di thanh "105,8172,20,9497" — OSRM doc ra bon so.
     */
    private String chuoiToaDo(List<OptimizationPoint> cacDiem) {
        StringJoiner chuoi = new StringJoiner(";");
        for (OptimizationPoint diem : cacDiem) {
            chuoi.add(String.format(Locale.ROOT, "%.7f,%.7f",
                    diem.getLongitude(), diem.getLatitude()));
        }
        return chuoi.toString();
    }

    /**
     * Doi mang Double[][] cua Jackson sang double[][] de thuat toan tra cho nhanh.
     *
     * OSRM tra ve null cho cap diem khong co duong di toi nhau (dao, khu vuc chua
     * co du lieu ban do). Dat NaN cho o do, DistanceMatrixService se lap bang Haversine —
     * de 0 thi thuat toan tuong hai diem trung nhau va tuyen ra sai hoan toan.
     */
    private BangKhoangCach doiSangMang(TableResponse traLoi, int n) {
        double[][] met = new double[n][n];
        int[][] giay = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Double khoangCach = traLoi.distances()[i][j];
                met[i][j] = (khoangCach == null) ? Double.NaN : khoangCach;

                if (traLoi.durations() != null && traLoi.durations()[i][j] != null) {
                    giay[i][j] = (int) Math.round(traLoi.durations()[i][j]);
                }
            }
        }
        return new BangKhoangCach(met, giay);
    }

    /** Phan JSON cua OSRM ma do an dung toi. Cac truong khac bo qua. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TableResponse(String code, Double[][] distances, Double[][] durations) {}
}
