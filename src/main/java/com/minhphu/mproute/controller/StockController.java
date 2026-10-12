package com.minhphu.mproute.controller;

import com.minhphu.mproute.entity.StockMovement;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.entity.enums.StockMovementType;
import com.minhphu.mproute.repository.ProductRepository;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.ReportExcelExporter;
import com.minhphu.mproute.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * SCR-21 / SCR-22 — Ton kho thanh pham va so kho (CN-01-10, CN-01-11).
 *
 * <h2>Phan quyen — co y khac voi cac man hinh danh muc khac</h2>
 *
 * Ma tran trong docs/01 cho dieu phoi vien CHI XEM danh muc. Ton kho thi khac: nhap kho
 * la viec VAN HANH lam hang ngay, giong don hang chu khong giong danh muc. Bat quan tri
 * vien dang nhap moi ngay chi de go "san xuat xong 500 thung" la sai nguoi — nguoi ngoi
 * ban ca ngay nghe dien thoai la dieu phoi vien.
 *
 * Mot cau luat cho ca man hinh nay:
 * <b>dieu phoi vien ghi hang vao kho, chi quan tri vien duoc sua lai so ton.</b>
 *
 * Nguoi go so hang ngay khong nen la nguoi co quyen am tham viet lai con so do: go nham
 * 500 thanh 5000 thi cach chua dung la bao len de kiem ke co ly do ghi so, khong phai tu
 * keo con so ve roi khong ai biet.
 *
 * <p>Cach dat annotation giong {@link ProductController}: annotation tren lop cho ca hai
 * vai tro, cac method sua so ton dat lai rieng cho ADMIN. Luu y annotation tren METHOD
 * <b>thay the</b> chu khong cong don voi annotation tren LOP — nen dong tren method la
 * chot cuoi cung. SecurityConfig cung da chan /admin/** o muc duong dan; giu ca hai lop
 * la co y (CBR-15).
 */
@Controller
@RequestMapping("/admin/stock")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class StockController {

    private static final int MOVEMENTS_PAGE_SIZE = 25;

    private final StockService stockService;
    private final ProductRepository productRepository;
    private final ReportExcelExporter reportExcelExporter;

    // ==================== CN-01-10 Man hinh ton kho ====================

    @GetMapping
    public String levels(Model model) {
        model.addAttribute("levels", stockService.levels());

        /*
         * Doi chieu ngay khi mo man hinh: cong ca so kho co ra dung so ton dang luu khong.
         * Khop thi khong hien gi, lech thi hien canh bao do — lech la loi trong code chu
         * khong phai loi nhap lieu, nen phai thay ngay chu khong de am tham.
         */
        model.addAttribute("reconcileProblems", stockService.reconcileAll());
        return "admin/stock";
    }

    /**
     * Nhap kho — dieu phoi vien lam duoc, thua huong phan quyen cua lop.
     */
    @PostMapping("/receive")
    public String receive(@RequestParam Long productId,
                          @RequestParam Integer boxCount,
                          @RequestParam(required = false) String note,
                          @AuthenticationPrincipal CurrentUser currentUser,
                          RedirectAttributes redirectAttributes) {

        if (boxCount == null || boxCount <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Số thùng nhập kho phải là số nguyên lớn hơn 0.");
            return "redirect:/admin/stock";
        }

        stockService.receive(productId, boxCount, blankToNull(note), currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã nhập kho " + boxCount + " thùng.");
        return "redirect:/admin/stock";
    }

    /**
     * Kiem ke — CHI quan tri vien. Dong @PreAuthorize nay thay the dong tren lop.
     *
     * Nguoi dung go SO THUNG DEM DUOC TRONG KHO, khong phai so con ranh. Phep tru do
     * StockService lam, vi trong so dem duoc co ca phan da cam ket cho don.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/stocktake")
    public String stocktake(@RequestParam Long productId,
                            @RequestParam Integer countedBoxes,
                            @RequestParam(required = false) String note,
                            @AuthenticationPrincipal CurrentUser currentUser,
                            RedirectAttributes redirectAttributes) {

        if (countedBoxes == null || countedBoxes < 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Số thùng đếm được phải là số nguyên không âm.");
            return "redirect:/admin/stock";
        }
        if (note == null || note.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Kiểm kê bắt buộc nhập lý do để sổ kho còn dấu vết.");
            return "redirect:/admin/stock";
        }

        stockService.stocktake(productId, countedBoxes, note.trim(), currentUser.getId())
                .ifPresentOrElse(
                        message -> redirectAttributes.addFlashAttribute("errorMessage", message),
                        () -> redirectAttributes.addFlashAttribute("successMessage",
                                "Đã ghi điều chỉnh kiểm kê vào sổ kho."));
        return "redirect:/admin/stock";
    }

    // ==================== CN-01-11 So kho ====================

    @GetMapping("/movements")
    public String movements(@RequestParam(required = false) Long productId,
                            @RequestParam(required = false) StockMovementType movementType,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {

        Page<StockMovement> pageResult = stockService.searchMovements(
                productId, movementType, startOfDay(fromDate), endOfDay(toDate),
                PageRequest.of(page, MOVEMENTS_PAGE_SIZE));

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("products", productRepository.findByStatus(RecordStatus.ACTIVE));
        model.addAttribute("movementTypes", StockMovementType.values());
        model.addAttribute("productId", productId);
        model.addAttribute("movementType", movementType);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "admin/stock-movements";
    }

    /**
     * Hoan tac mot dong so kho — CHI quan tri vien.
     *
     * Khong xoa dong goc (CBR-17): ghi mot dong DAO tro ve no. Nguoi dung thay nhu da xoa,
     * nhung van con dau vet ai ghi sai gi luc nao.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/movements/{id}/reverse")
    public String reverse(@PathVariable Long id,
                          @RequestParam(required = false) String note,
                          @AuthenticationPrincipal CurrentUser currentUser,
                          RedirectAttributes redirectAttributes) {

        if (note == null || note.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Hoàn tác bắt buộc nhập lý do.");
            return "redirect:/admin/stock/movements";
        }

        try {
            stockService.reverse(id, note.trim(), currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã ghi dòng đảo. Dòng gốc được giữ lại trong sổ.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/stock/movements";
    }

    /** Xuat so kho ra Excel, giu dung bo loc dang xem tren man hinh. */
    @GetMapping("/movements/excel")
    public ResponseEntity<byte[]> exportMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) StockMovementType movementType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        List<StockMovement> movements = stockService.searchAllMovements(
                productId, movementType, startOfDay(fromDate), endOfDay(toDate));

        // Khoang ngay chi de in len dau file. Khong loc thi lay ngay cua dong dau va dong
        // cuoi de tieu de file khop voi noi dung ben trong
        LocalDate from = (fromDate != null) ? fromDate : earliestDate(movements);
        LocalDate to = (toDate != null) ? toDate : LocalDate.now();

        byte[] workbook = reportExcelExporter.exportStockLedger(movements, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ReportExcelExporter.XLSX_CONTENT_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ReportExcelExporter.fileName("so-kho", from, to) + "\"")
                .body(workbook);
    }

    // ==================== Ham tien ich ====================

    /**
     * Loc theo ngay phai bao tron ca ngay: dieu kien >= 00:00 va <= 23:59:59.
     * Lay thang LocalDate di so sanh voi LocalDateTime thi cac dong trong ngay cuoi
     * se bi loai, vi 2026-10-11T14:30 lon hon 2026-10-11T00:00.
     */
    private LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }

    private LocalDate earliestDate(List<StockMovement> movements) {
        // Danh sach da sap giam dan theo thoi gian nen dong cuoi la dong cu nhat
        return movements.isEmpty()
                ? LocalDate.now()
                : movements.get(movements.size() - 1).getCreatedAt().toLocalDate();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
