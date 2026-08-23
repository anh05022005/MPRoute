package com.minhphu.mproute.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minhphu.mproute.dto.GeocodingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tim toa do tu dia chi bang Nominatim (CN-01-05, BR01-05, BR01-06).
 *
 * Vi sao goi qua may chu chu khong goi thang tu trinh duyet:
 *   1. Nominatim yeu cau moi request khai bao User-Agent nhan dang duoc ung dung.
 *      Goi tu JavaScript thi trinh duyet tu dat User-Agent, khong sua duoc.
 *   2. Ho gioi han 1 request moi giay. Chan o may chu thi chan duoc that,
 *      chan trong JavaScript thi mo hai tab la vuot.
 *   3. Khong phu thuoc vao viec Nominatim co tra header CORS hay khong.
 */
@Service
@Slf4j
public class GeocodingService {

    // Nominatim yeu cau toi thieu 1 giay giua hai request tu cung mot nguon
    private static final Duration NHIP_TOI_THIEU = Duration.ofSeconds(1);

    private static final int SO_KET_QUA_TOI_DA = 5;

    private final RestClient restClient;
    private final String serverUrl;
    private final String hanoiViewBox;

    // Thoi diem goi Nominatim gan nhat, tinh bang nanoTime de khong bi anh huong
    // khi dong ho he thong bi chinh
    private long lastCallNs = 0L;

    public GeocodingService(
            @Value("${mproute.nominatim.url:https://nominatim.openstreetmap.org}") String serverUrl,
            @Value("${mproute.nominatim.user-agent:MPRoute-DoAnTotNghiep/1.0}") String userAgent,
            @Value("${mproute.nominatim.viewbox:105.28,21.39,106.05,20.53}") String hanoiViewBox) {

        this.serverUrl = serverUrl;
        this.hanoiViewBox = hanoiViewBox;
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                // Chinh sach su dung cua Nominatim bat buoc co dong nay
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Language", "vi")
                .build();
    }

    /**
     * Tra ve danh sach dia diem khop voi chuoi tim.
     *
     * Danh sach RONG la truong hop hop le, khong phai loi: BR01-06 noi khong tim
     * duoc thi bat nguoi dung ghim tay tren ban do. Loi mang cung tra ve danh sach
     * rong — o goc nhin cua man hinh thi hai truong hop nay xu ly nhu nhau.
     */
    public List<GeocodingResult> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        throttle();

        try {
            NominatimItem[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", keyword.trim())
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", SO_KET_QUA_TOI_DA)
                            // Gioi han trong Viet Nam: khach hang cua Minh Phu deu o Ha Noi,
                            // bo loc nay cat han cac dia danh trung ten o nuoc ngoai
                            .queryParam("countrycodes", "vn")
                            /*
                             * Chi lay ket qua nam trong khung bao quanh Ha Noi.
                             *
                             * Khong co hai tham so nay thi Nominatim doan bua sang tinh khac:
                             * "15 Kim Giang, Thanh Xuan" tra ve dung MOT ket qua la duong
                             * Nguyen Kim o DA NANG, cach 800 km. Mot ket qua sai duy nhat
                             * nguy hiem hon khong co ket qua nao, vi nguoi dung de bam nham.
                             *
                             * Da do: khung nay khong lam mat ket qua dung nao — "24 Lang Ha"
                             * va "Phan Trong Tue" van ra du 5 ket qua o Ha Noi.
                             */
                            .queryParam("viewbox", hanoiViewBox)
                            .queryParam("bounded", 1)
                            .build())
                    .retrieve()
                    .body(NominatimItem[].class);

            List<GeocodingResult> results = toResult(response);
            log.info("Tim toa do \"{}\" — {} ket qua", keyword, results.size());
            return results;

        } catch (Exception ex) {
            /*
             * Nuot ngoai le co chu dich. Nominatim la dich vu ngoai, no co the cham,
             * chan IP, hoac may khong co mang. Khong duoc de mot loi mang lam hong
             * ca man hinh them khach hang — nguoi dung van phai nhap toa do tay duoc.
             */
            log.warn("Goi Nominatim that bai ({}): {}", serverUrl, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Doi phan hoi tho cua Nominatim sang kieu dung trong ung dung.
     *
     * Bo qua dong nao thieu toa do hoac toa do khong doc duoc thanh so, thay vi
     * lam hong ca danh sach — mot dong hong khong duoc keo theo bon dong con lai.
     */
    private List<GeocodingResult> toResult(NominatimItem[] response) {
        if (response == null) {
            return List.of();
        }
        List<GeocodingResult> results = new ArrayList<>();
        for (NominatimItem item : response) {
            if (item.lat() == null || item.lon() == null) {
                continue;
            }
            try {
                results.add(new GeocodingResult(
                        item.displayName(),
                        Double.parseDouble(item.lat()),
                        Double.parseDouble(item.lon())));
            } catch (NumberFormatException ex) {
                log.warn("Nominatim tra toa do khong doc duoc: {} / {}", item.lat(), item.lon());
            }
        }
        return results;
    }

    /**
     * Cho cho du 1 giay ke tu lan goi truoc.
     *
     * synchronized vi nhieu nguoi dung bam cung luc van la nhieu luong trong
     * cung mot ung dung — Nominatim nhin thay chung la mot nguon.
     */
    private synchronized void throttle() {
        long elapsedNs = System.nanoTime() - lastCallNs;
        long remainingMs = NHIP_TOI_THIEU.toMillis() - (elapsedNs / 1_000_000L);
        if (lastCallNs > 0 && remainingMs > 0) {
            try {
                Thread.sleep(remainingMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallNs = System.nanoTime();
    }

    /** Chi lay ba truong can dung, bo qua phan con lai cua phan hoi Nominatim. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimItem(
            String lat,
            String lon,
            @JsonProperty("display_name") String displayName) {
    }
}
