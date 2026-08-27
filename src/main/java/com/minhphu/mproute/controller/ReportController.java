package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.OptimizationReport;
import com.minhphu.mproute.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SCR-14 — Man hinh bao cao (nhom 8).
 *
 * Hien co mot bao cao: RPT-03 hieu qua toi uu (CN-08-03). Duong dan de o
 * /reports/optimization ngay tu dau de sau nay them RPT-01, RPT-02 khong phai doi
 * dia chi cu — dia chi da vao slide va vao video demo thi doi la phai lam lai het.
 *
 * Ca quan tri lan dieu phoi deu xem duoc (docs/05 — SCR-14). Chan hai lop: mot lop o
 * SecurityConfig cho /reports/**, mot lop @PreAuthorize ngay tren lop nay — an nut tren
 * menu khong phai la chan (CBR-15).
 */
@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    /** Mo bao cao khong chon ngay thi xem 30 ngay gan nhat — du dai de thay xu the. */
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final ReportService reportService;

    @GetMapping
    public String index() {
        return "redirect:/reports/optimization";
    }

    /**
     * RPT-03 — bao cao hieu qua toi uu (CN-08-03).
     *
     * Hai tham so ngay deu khong bat buoc: bam tu menu la vao thang duoc, khong phai
     * dien gi truoc. Ngay ket thuc mac dinh la HOM NAY chu khong phai ngay mai, vi bao
     * cao nhin ve qua khu.
     */
    @GetMapping("/optimization")
    public String optimizationReport(@RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                     Model model) {

        LocalDate to = (toDate != null) ? toDate : LocalDate.now();
        LocalDate from = (fromDate != null) ? fromDate : to.minusDays(DEFAULT_RANGE_DAYS - 1L);

        // Nhap nguoc hai dau thi doi cho cho nhau, khong bat nguoi dung nhap lai
        if (from.isAfter(to)) {
            LocalDate swapped = from;
            from = to;
            to = swapped;
        }

        /*
         * BR08-01 — khoang toi da 365 ngay. Cat bot roi bao ro thay vi tu choi hien gi:
         * nguoi dung go nham nam vao o ngay bat dau van thay duoc bao cao, va van biet
         * la he thong da cat.
         */
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > ReportService.MAX_RANGE_DAYS) {
            from = to.minusDays(ReportService.MAX_RANGE_DAYS - 1L);
            model.addAttribute("errorMessage",
                    "Khoảng thời gian tối đa của một báo cáo là " + ReportService.MAX_RANGE_DAYS
                            + " ngày. Hệ thống đã tự rút gọn về khoảng này.");
        }

        OptimizationReport report = reportService.optimizationReport(from, to);
        model.addAttribute("report", report);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);

        return "reports/optimization";
    }
}
