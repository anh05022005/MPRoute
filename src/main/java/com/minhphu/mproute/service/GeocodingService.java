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
    private final String diaChiMayChu;

    // Thoi diem goi Nominatim gan nhat, tinh bang nanoTime de khong bi anh huong
    // khi dong ho he thong bi chinh
    private long lanGoiTruoc = 0L;

    public GeocodingService(
            @Value("${mproute.nominatim.url:https://nominatim.openstreetmap.org}") String diaChiMayChu,
            @Value("${mproute.nominatim.user-agent:MPRoute-DoAnTotNghiep/1.0}") String userAgent) {

        this.diaChiMayChu = diaChiMayChu;
        this.restClient = RestClient.builder()
                .baseUrl(diaChiMayChu)
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
    public List<GeocodingResult> search(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return List.of();
        }

        choDuNhip();

        try {
            NominatimItem[] phanHoi = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", tuKhoa.trim())
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", SO_KET_QUA_TOI_DA)
                            // Gioi han trong Viet Nam: khach hang cua Minh Phu deu o Ha Noi,
                            // bo loc nay cat han cac dia danh trung ten o nuoc ngoai
                            .queryParam("countrycodes", "vn")
                            .build())
                    .retrieve()
                    .body(NominatimItem[].class);

            if (phanHoi == null) {
                return List.of();
            }

            List<GeocodingResult> ketQua = new ArrayList<>();
            for (NominatimItem item : phanHoi) {
                if (item.lat() == null || item.lon() == null) {
                    continue;
                }
                try {
                    ketQua.add(new GeocodingResult(
                            item.displayName(),
                            Double.parseDouble(item.lat()),
                            Double.parseDouble(item.lon())));
                } catch (NumberFormatException loi) {
                    log.warn("Nominatim tra toa do khong doc duoc: {} / {}", item.lat(), item.lon());
                }
            }
            log.info("Tim toa do \"{}\" — {} ket qua", tuKhoa, ketQua.size());
            return ketQua;

        } catch (Exception loi) {
            /*
             * Nuot ngoai le co chu dich. Nominatim la dich vu ngoai, no co the cham,
             * chan IP, hoac may khong co mang. Khong duoc de mot loi mang lam hong
             * ca man hinh them khach hang — nguoi dung van phai nhap toa do tay duoc.
             */
            log.warn("Goi Nominatim that bai ({}): {}", diaChiMayChu, loi.getMessage());
            return List.of();
        }
    }

    /**
     * Cho cho du 1 giay ke tu lan goi truoc.
     *
     * synchronized vi nhieu nguoi dung bam cung luc van la nhieu luong trong
     * cung mot ung dung — Nominatim nhin thay chung la mot nguon.
     */
    private synchronized void choDuNhip() {
        long daTroiNs = System.nanoTime() - lanGoiTruoc;
        long conThieuMs = NHIP_TOI_THIEU.toMillis() - (daTroiNs / 1_000_000L);
        if (lanGoiTruoc > 0 && conThieuMs > 0) {
            try {
                Thread.sleep(conThieuMs);
            } catch (InterruptedException loi) {
                Thread.currentThread().interrupt();
            }
        }
        lanGoiTruoc = System.nanoTime();
    }

    /** Chi lay ba truong can dung, bo qua phan con lai cua phan hoi Nominatim. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimItem(
            String lat,
            String lon,
            @JsonProperty("display_name") String displayName) {
    }
}
