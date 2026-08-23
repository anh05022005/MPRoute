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
    private static final int MAX_TABLE_POINTS = 100;

    private final String serverUrl;
    private final RestClient restClient;

    public OsrmDistanceCalculator(String serverUrl, int timeoutSeconds) {
        this.serverUrl = serverUrl;

        /*
         * Dat timeout la BAT BUOC. Mac dinh cua Java la cho vo han: OSRM treo giua
         * chung thi man hinh phan tuyen quay mai khong bao gio bao loi, ma dung
         * phuong an du phong Haversine thi da xong tu lau.
         */
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /** Ma tran khoang cach (met) va thoi gian di chuyen (giay) cua cung mot tap diem. */
    public record DistanceTable(double[][] meters, int[][] seconds) {}

    /**
     * Lay ca ma tran trong mot lan goi.
     *
     * @return rong khi OSRM khong chay, tra loi, hoac vuot gioi han so diem —
     *         goi ben ngoai se lui ve Haversine (CBR-11). Day KHONG phai loi
     *         chuong trinh, la truong hop da luong truoc.
     */
    public Optional<DistanceTable> fetchDistanceTable(List<OptimizationPoint> points) {
        if (points == null || points.isEmpty()) {
            return Optional.empty();
        }
        if (points.size() > MAX_TABLE_POINTS) {
            log.warn("Co {} diem, vuot gioi han {} cua OSRM Table API. Dung Haversine.",
                    points.size(), MAX_TABLE_POINTS);
            return Optional.empty();
        }

        String path = "/table/v1/driving/" + toCoordinateString(points)
                + "?annotations=distance,duration";

        try {
            TableResponse response = restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(TableResponse.class);

            if (response == null || !"Ok".equals(response.code()) || response.distances() == null) {
                log.warn("OSRM tra ve khong hop le: {}", response == null ? "null" : response.code());
                return Optional.empty();
            }

            return Optional.of(toPrimitiveArrays(response, points.size()));

        } catch (Exception ex) {
            /*
             * Nuot loi CO CHU DICH. OSRM khong chay la truong hop binh thuong tren may
             * chua dung Docker, va CBR-11 da co phuong an du phong. Ghi log de con biet
             * ma tran nay la xap xi chu khong phai duong di that.
             */
            log.warn("Khong goi duoc OSRM tai {}: {}. Se dung Haversine.",
                    serverUrl, ex.getMessage());
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
    public double distanceBetween(OptimizationPoint from, OptimizationPoint to) {
        return fetchDistanceTable(List.of(from, to))
                .map(table -> table.meters()[0][1])
                .orElse(0.0);
    }

    @Override
    public String sourceName() {
        return "OSRM";
    }

    /** Kiem OSRM co dang chay khong, dung de hien trang thai tren man hinh phan tuyen. */
    public boolean isAvailable() {
        OptimizationPoint depot = OptimizationPoint.depot(20.9497, 105.8172);
        return fetchDistanceTable(List.of(depot, depot)).isPresent();
    }

    // ==================== Ham rieng ====================

    /**
     * Ghep danh sach diem thanh chuoi "lon,lat;lon,lat;..." theo dung khuon OSRM.
     *
     * Locale.ROOT la BAT BUOC: may cai dat tieng Viet dinh dang so thap phan bang
     * DAU PHAY, ma dau phay lai chinh la ky tu ngan cach lon voi lat. Khong dat
     * locale thi chuoi gui di thanh "105,8172,20,9497" — OSRM doc ra bon so.
     */
    private String toCoordinateString(List<OptimizationPoint> points) {
        StringJoiner joiner = new StringJoiner(";");
        for (OptimizationPoint point : points) {
            joiner.add(String.format(Locale.ROOT, "%.7f,%.7f",
                    point.getLongitude(), point.getLatitude()));
        }
        return joiner.toString();
    }

    /**
     * Doi mang Double[][] cua Jackson sang double[][] de thuat toan tra cho nhanh.
     *
     * OSRM tra ve null cho cap diem khong co duong di toi nhau (dao, khu vuc chua
     * co du lieu ban do). Dat NaN cho o do, DistanceMatrixService se lap bang Haversine —
     * de 0 thi thuat toan tuong hai diem trung nhau va tuyen ra sai hoan toan.
     */
    private DistanceTable toPrimitiveArrays(TableResponse response, int size) {
        double[][] meters = new double[size][size];
        int[][] seconds = new int[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Double distance = response.distances()[i][j];
                meters[i][j] = (distance == null) ? Double.NaN : distance;

                if (response.durations() != null && response.durations()[i][j] != null) {
                    seconds[i][j] = (int) Math.round(response.durations()[i][j]);
                }
            }
        }
        return new DistanceTable(meters, seconds);
    }

    /** Phan JSON cua OSRM ma do an dung toi. Cac truong khac bo qua. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TableResponse(String code, Double[][] distances, Double[][] durations) {}
}
