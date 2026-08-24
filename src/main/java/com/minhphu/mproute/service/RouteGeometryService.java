package com.minhphu.mproute.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.minhphu.mproute.entity.Depot;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Lay duong di THAT cua tung tuyen de ve len ban do (CN-03-09).
 *
 * DIEM DE SAI NHAT CUA MAN HINH PHAN TUYEN, docs/05 noi thang: KHONG duoc noi thang toa do
 * cac khach voi nhau. Noi thang thi duong ve cat qua song Hong, cat qua ho Tay, va nhin la
 * biet ngay he thong khong dung duong di that — trong khi so km ben canh lai lay tu OSRM.
 * Hai thu vênh nhau la cau hoi dau tien cua hoi dong.
 *
 * Dung Route API chu khong phai Table API:
 *
 *     GET /route/v1/driving/{lon,lat};{lon,lat};...?overview=full&geometries=geojson
 *
 * Table API chi tra ve CON SO khoang cach, khong tra ve hinh dang duong di. Hai API khac
 * viec nhau: Table de thuat toan tinh, Route de nguoi nhin.
 *
 * VI SAO LOP NAY KHONG NAM O service/optimization: no khong tham gia vao viec giai bai toan.
 * Thuat toan da chay xong, tuyen da chot thu tu diem roi moi goi toi day chi de ve. Goi mot
 * lan cho MOT tuyen, ngoai moi vong lap thuat toan — khong pham dieu cam so 4.
 *
 * KHONG CO BO NHO DEM. Moi lan mo man hinh phan tuyen la mot lan goi OSRM cho moi tuyen,
 * tuc 1-5 request toi localhost. Chap nhan duoc vi day la man hinh mot nguoi dung, va bang
 * distance_matrix chi thiet ke de dem KHOANG CACH chu khong dem hinh dang duong di.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteGeometryService {

    /**
     * Bang mau tuyen, phai khop bang mau o docs/05-giao-dien.md va cac bien --route-1..5
     * trong static/css/style.css. Tuyen thu 6 quay lai mau dau — thuc te Minh Phu co 4 xe.
     */
    private static final List<String> ROUTE_COLORS =
            List.of("#1E77BF", "#C75B39", "#1B8A5A", "#7D5BA6", "#B7791F");

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private final SystemParameterService systemParameterService;

    /** Mot diem dung de ve ghim danh so tren ban do. */
    public record MapStop(int sequence,
                          String customerName,
                          String orderCode,
                          double latitude,
                          double longitude,
                          String estimatedArrival) {}

    /**
     * Mot tuyen da san sang ve: mau, cac ghim danh so, va duong di.
     *
     * path la mang cac cap [vi do, kinh do] theo DUNG thu tu Leaflet can. OSRM tra ve
     * [kinh do, vi do] nen o day da dao lai roi — dao o may chu mot lan, de JavaScript
     * khong phai nho quy uoc nguoc doi nay.
     *
     * approximate = true nghia la duong nay chi la doan thang noi cac diem vi OSRM khong
     * tra loi. Ban do se ve net dut de nhin la biet ngay, khong duoc ve giong duong that.
     */
    public record RouteLine(String code,
                            String licensePlate,
                            String color,
                            boolean approximate,
                            List<MapStop> stops,
                            List<double[]> path) {}

    /**
     * Dung du lieu ve cho toan bo cac tuyen cua mot ngay.
     *
     * @param depot kho, dung lam diem dau VA diem cuoi cua moi tuyen — duong ve kho cung
     *              phai duoc ve ra, no la mot phan cua quang duong da tinh (CBR-01)
     */
    public List<RouteLine> buildRouteLines(Depot depot, List<Route> routes) {
        List<RouteLine> lines = new ArrayList<>();
        if (depot == null) {
            return lines;
        }

        RestClient osrmClient = newOsrmClient();

        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            if (route.getStops().isEmpty()) {
                continue;
            }

            // Kho -> cac diem giao theo thu tu -> KHO. Chang cuoi ve kho phai co mat
            List<double[]> waypoints = new ArrayList<>();
            waypoints.add(new double[]{depot.getLatitude(), depot.getLongitude()});
            for (RouteStop stop : route.getStops()) {
                waypoints.add(new double[]{stop.getOrder().getLatitude(), stop.getOrder().getLongitude()});
            }
            waypoints.add(new double[]{depot.getLatitude(), depot.getLongitude()});

            Optional<List<double[]>> realPath = fetchPath(osrmClient, waypoints);

            lines.add(new RouteLine(
                    route.getCode(),
                    route.getVehicle().getLicensePlate(),
                    ROUTE_COLORS.get(i % ROUTE_COLORS.size()),
                    realPath.isEmpty(),
                    toMapStops(route),
                    realPath.orElse(waypoints)));
        }
        return lines;
    }

    // ==================== Ham rieng ====================

    /**
     * Goi OSRM Route API lay hinh dang duong di.
     *
     * @return rong khi OSRM khong chay hoac khong tim duoc duong — goi ben ngoai se ve
     *         tam doan thang va danh dau la xap xi (CBR-11). Day la truong hop DA LUONG
     *         TRUOC, khong phai loi chuong trinh
     */
    private Optional<List<double[]>> fetchPath(RestClient osrmClient, List<double[]> waypoints) {
        String path = "/route/v1/driving/" + toCoordinateString(waypoints)
                + "?overview=full&geometries=geojson";

        try {
            RouteResponse response = osrmClient.get()
                    .uri(path)
                    .retrieve()
                    .body(RouteResponse.class);

            if (response == null || !"Ok".equals(response.code())
                    || response.routes() == null || response.routes().isEmpty()) {
                log.warn("OSRM Route API tra ve khong hop le: {}",
                        response == null ? "null" : response.code());
                return Optional.empty();
            }

            List<List<Double>> coordinates = response.routes().get(0).geometry().coordinates();
            if (coordinates == null || coordinates.isEmpty()) {
                return Optional.empty();
            }

            /*
             * DAO THU TU TOA DO. OSRM tra ve [kinh do, vi do] theo chuan GeoJSON, con
             * Leaflet nhan [vi do, kinh do]. Quen dao thi khong bao loi gi ca, duong chi
             * hien o giua Somalia — loi im lang kinh dien khi lam ban do.
             */
            List<double[]> leafletPath = new ArrayList<>(coordinates.size());
            for (List<Double> pair : coordinates) {
                leafletPath.add(new double[]{pair.get(1), pair.get(0)});
            }
            return Optional.of(leafletPath);

        } catch (Exception ex) {
            // Nuot loi co chu dich: OSRM tat la truong hop binh thuong, da co phuong an du phong
            log.warn("Khong lay duoc duong di tu OSRM: {}. Se ve tam doan thang.", ex.getMessage());
            return Optional.empty();
        }
    }

    private List<MapStop> toMapStops(Route route) {
        List<MapStop> stops = new ArrayList<>();
        for (RouteStop stop : route.getStops()) {
            stops.add(new MapStop(
                    stop.getSequence(),
                    stop.getOrder().getCustomer().getName(),
                    stop.getOrder().getCode(),
                    stop.getOrder().getLatitude(),
                    stop.getOrder().getLongitude(),
                    stop.getEstimatedArrival() == null ? null
                            : stop.getEstimatedArrival().format(HOUR_MINUTE)));
        }
        return stops;
    }

    /** Tao bo goi OSRM theo tham so he thong, de doi dia chi khong phai bien dich lai. */
    private RestClient newOsrmClient() {
        String serverUrl = systemParameterService.getValue(SystemParameterService.OSRM_URL);
        int timeoutSeconds = systemParameterService.getInt(SystemParameterService.OSRM_TIMEOUT);

        /*
         * Dat timeout la bat buoc, giong OsrmDistanceCalculator: mac dinh cua Java la cho
         * vo han, OSRM treo giua chung thi man hinh phan tuyen quay mai khong bao gio ve.
         */
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .baseUrl(serverUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Ghep cac diem thanh chuoi "lon,lat;lon,lat;..." theo dung khuon OSRM.
     *
     * Locale.ROOT la BAT BUOC: may cai dat tieng Viet dinh dang so thap phan bang DAU PHAY,
     * ma dau phay lai chinh la ky tu ngan cach kinh do voi vi do. Khong dat locale thi chuoi
     * gui di thanh "105,8172,20,9497" — OSRM doc ra bon so.
     */
    private String toCoordinateString(List<double[]> waypoints) {
        StringJoiner joiner = new StringJoiner(";");
        for (double[] point : waypoints) {
            // point la [vi do, kinh do], OSRM can nguoc lai
            joiner.add(String.format(Locale.ROOT, "%.7f,%.7f", point[1], point[0]));
        }
        return joiner.toString();
    }

    /** Phan JSON cua OSRM Route API ma do an dung toi. Cac truong khac bo qua. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RouteResponse(String code, List<RoutePart> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RoutePart(Geometry geometry, Double distance) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Geometry(List<List<Double>> coordinates) {}
}
