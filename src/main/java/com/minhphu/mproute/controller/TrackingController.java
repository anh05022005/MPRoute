package com.minhphu.mproute.controller;

import com.minhphu.mproute.security.LookupRateLimiter;
import com.minhphu.mproute.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * SCR-13 — Trang tra cuu cong khai (CN-07-01 -> CN-07-07).
 *
 * DUONG DUY NHAT CUA HE THONG MO CHO NGUOI CHUA DANG NHAP. SecurityConfig cho /track qua
 * permitAll, nen moi rao chan phai tu lop nay va TrackingService dung ra — khong co
 * @PreAuthorize nao do ho o day duoc.
 *
 * Dung POST chu khong phai GET co chu dich: o nhap la SO DIEN THOAI cua khach. De GET thi so
 * do nam trong lich su trinh duyet cua may (co the la may o quan net) va trong log truy cap
 * cua may chu. POST thi khong. Doi lai la bam F5 se hoi gui lai — chap nhan duoc cho mot trang
 * tra cuu.
 */
@Controller
@RequestMapping("/track")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;
    private final LookupRateLimiter lookupRateLimiter;

    /** CN-07-01 — trang tra cuu, vao duoc khi chua dang nhap. */
    @GetMapping
    public String form() {
        return "track";
    }

    /**
     * Tra cuu thang mot ma don — duong dat trong email bao gio giao (BR04-11, CN-04-08).
     *
     * Day la duong DUY NHAT cua trang tra cuu dung GET, va chi nhan MA DON chu khong nhan so
     * dien thoai. Ma don di trong thu gui rieng cho chinh khach do nen dat len thanh dia chi
     * la chap nhan duoc; so dien thoai thi khong, vi no la thu tra duoc ra nhieu don va de
     * do tuan tu.
     */
    @GetMapping("/{code}")
    public String lookupByCode(@PathVariable String code,
                               HttpServletRequest request,
                               Model model) {
        return lookup(code, request, model);
    }

    /** CN-07-02 — tra theo ma don hoac so dien thoai. */
    @PostMapping
    public String lookup(@RequestParam(required = false) String query,
                         HttpServletRequest request,
                         Model model) {

        model.addAttribute("query", query);

        // CN-07-07, BR07-09 — chan truoc khi cham vao CSDL, khong de mot script quet lam viec
        if (!lookupRateLimiter.tryAcquire(clientAddressOf(request))) {
            model.addAttribute("errorMessage",
                    "Bạn tra cứu quá nhiều lần. Vui lòng thử lại sau một phút.");
            return "track";
        }

        List<TrackingService.TrackedOrder> orders = trackingService.lookup(query);
        model.addAttribute("orders", orders);

        /*
         * MSG-11 — mot cau chung chung cho MOI truong hop khong ra ket qua (BR07-08).
         *
         * Khong duoc tach thanh "ma don khong ton tai" va "so dien thoai chua co don nao":
         * phan biet hai cau do la bien trang nay thanh cong cu do xem so dien thoai nao tung
         * mua hang cua Minh Phu, chi bang cach thu lan luot.
         */
        if (orders.isEmpty()) {
            model.addAttribute("notFound", true);
        }
        return "track";
    }

    // ==================== Ham rieng ====================

    /**
     * Dia chi IP cua nguoi tra cuu, dung lam khoa dem tan suat.
     *
     * Doc X-Forwarded-For truoc vi neu ve sau co dat may chu nay sau mot proxy thi
     * getRemoteAddr() se tra ve dia chi cua proxy — luc do MOI nguoi dung chung mot so dem
     * va han muc 20 lan/phut se chan nham ca nhung khach binh thuong.
     *
     * Chay o localhost thi khong co header nay, va gia mao header cung khong lam duoc gi hon
     * ngoai viec tu cho minh mot so dem rieng — day la rao chong quet hang loat, khong phai
     * co che xac thuc.
     */
    private String clientAddressOf(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Chuoi co dang "client, proxy1, proxy2" — dia chi dau tien la cua nguoi dung
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
