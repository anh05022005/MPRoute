package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.DeliveryReport;
import com.minhphu.mproute.dto.OptimizationReport;
import com.minhphu.mproute.dto.OrderReport;
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
 * Ba bao cao, moi cai mot duong dan rieng:
 *   /reports/orders        RPT-01 thong ke don hang     (CN-08-01)
 *   /reports/delivery      RPT-02 ty le giao thanh cong (CN-08-02)
 *   /reports/optimization  RPT-03 hieu qua toi uu       (CN-08-03) — quan trong nhat
 *
 * /reports tro thang vao RPT-03 chu khong vao RPT-01: mo menu Bao cao ra la thay ngay
 * bao cao dang gia nhat, khong phai bam them mot lan nua.
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
     * RPT-01 — thong ke don hang (CN-08-01).
     *
     * grouping quyet dinh gom theo ngay, tuan hay thang. Mac dinh gom theo NGAY vi do la
     * cach nhin ma dieu phoi vien dung hang ngay; xem theo thang la viec cua cuoi ky.
     */
    @GetMapping("/orders")
    public String orderReport(@RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                              @RequestParam(required = false) OrderReport.Grouping grouping,
                              Model model) {

        LocalDate[] range = resolveRange(fromDate, toDate, model);
        OrderReport.Grouping chosen = (grouping != null) ? grouping : OrderReport.Grouping.DAY;

        model.addAttribute("report", reportService.orderReport(range[0], range[1], chosen));
        model.addAttribute("groupings", OrderReport.Grouping.values());
        model.addAttribute("selectedGrouping", chosen);
        addRange(model, range);
        return "reports/orders";
    }

    /** RPT-02 — ty le giao thanh cong va phan tich ly do that bai (CN-08-02). */
    @GetMapping("/delivery")
    public String deliveryReport(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                 Model model) {

        LocalDate[] range = resolveRange(fromDate, toDate, model);
        DeliveryReport report = reportService.deliveryReport(range[0], range[1]);

        model.addAttribute("report", report);
        addRange(model, range);
        return "reports/delivery";
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

        LocalDate[] range = resolveRange(fromDate, toDate, model);

        OptimizationReport report = reportService.optimizationReport(range[0], range[1]);
        model.addAttribute("report", report);
        addRange(model, range);

        return "reports/optimization";
    }

    /**
     * Chuan hoa khoang ngay cho ca ba bao cao — dung chung nen ba man hinh khong bao gio
     * hieu tham so khac nhau.
     *
     * @return mang hai phan tu: [tu ngay, den ngay]
     */
    private LocalDate[] resolveRange(LocalDate fromDate, LocalDate toDate, Model model) {

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
        return new LocalDate[]{from, to};
    }

    private void addRange(Model model, LocalDate[] range) {
        model.addAttribute("fromDate", range[0]);
        model.addAttribute("toDate", range[1]);
    }
}
