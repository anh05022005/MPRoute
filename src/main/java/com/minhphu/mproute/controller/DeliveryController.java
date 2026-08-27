package com.minhphu.mproute.controller;

import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.enums.FailureReason;
import com.minhphu.mproute.entity.enums.IncidentSeverity;
import com.minhphu.mproute.entity.enums.IncidentType;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.DeliveryService;
import com.minhphu.mproute.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * SCR-12 — Man hinh tai xe tren dien thoai (CN-05-01 -> CN-05-05).
 *
 * KHONG CO THAM SO NAO NHAN ID TUYEN. Moi duong o day deu di tu tai khoan dang dang nhap ma
 * tim ra tuyen, chu khong nhan id tu ben ngoai roi di tim. Nho vay khong ton tai duong nao de
 * tro toi tuyen cua tai xe khac, ke ca khi doan dung id (BR05-01, CBR-15).
 *
 * Chi vai tro DRIVER vao duoc. SecurityConfig da chan /driver/** o muc duong dan, o day khai
 * them @PreAuthorize de neu ai do sua SecurityConfig thi van con mot lop nua.
 */
@Controller
@RequestMapping("/driver")
@PreAuthorize("hasRole('DRIVER')")
@RequiredArgsConstructor
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final IncidentService incidentService;

    /** CN-05-01 — tuyen hom nay cua chinh tai xe dang dang nhap. */
    @GetMapping("/my-route")
    public String myRoute(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        fillModel(currentUser, model);
        return "driver/my-route";
    }

    /** CN-05-11 — cac tuyen tai xe da chay nhung ngay truoc. */
    @GetMapping("/history")
    public String history(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("history", deliveryService.historyOf(currentUser.getId()));
        return "driver/history";
    }

    /** CN-05-02 — xac nhan da nhan hang, tuyen chuyen sang Dang chay (BR05-03). */
    @PostMapping("/my-route/pickup")
    public String confirmPickup(@AuthenticationPrincipal CurrentUser currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            Route route = deliveryService.confirmPickup(currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "Đã xác nhận nhận hàng cho tuyến %s. Chúc bạn đi đường an toàn.",
                    route.getCode()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/my-route";
    }

    /** CN-05-06, CN-05-07 — cap nhat da giao, kem so tien thuc thu neu don phai thu. */
    @PostMapping("/my-route/stops/{stopId}/delivered")
    public String markDelivered(@PathVariable Long stopId,
                                @RequestParam(required = false) BigDecimal collectedAmount,
                                @RequestParam(required = false) String collectionNote,
                                @RequestParam(required = false) Double latitude,
                                @RequestParam(required = false) Double longitude,
                                @AuthenticationPrincipal CurrentUser currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            var stop = deliveryService.markDelivered(currentUser.getId(), stopId,
                    collectedAmount, collectionNote, latitude, longitude);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã ghi nhận giao thành công cho " + stop.getOrder().getCustomer().getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/my-route";
    }

    /** CN-05-08 — giao that bai, bat buoc chon ly do (BR05-06). */
    @PostMapping("/my-route/stops/{stopId}/failed")
    public String markFailed(@PathVariable Long stopId,
                             @RequestParam(required = false) FailureReason failureReason,
                             @RequestParam(required = false) String failureNote,
                             @RequestParam(required = false) Double latitude,
                             @RequestParam(required = false) Double longitude,
                             @AuthenticationPrincipal CurrentUser currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            var stop = deliveryService.markFailed(currentUser.getId(), stopId,
                    failureReason, failureNote, latitude, longitude);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã báo giao thất bại cho " + stop.getOrder().getCustomer().getName()
                            + ". Điều phối viên đã nhận được thông báo.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/my-route";
    }

    /** CN-05-09 — hoan sang ngay khac, don duoc tao lai o ngay moi (BR05-07). */
    @PostMapping("/my-route/stops/{stopId}/postpone")
    public String postpone(@PathVariable Long stopId,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDeliveryDate,
                           @RequestParam(required = false) String note,
                           @RequestParam(required = false) Double latitude,
                           @RequestParam(required = false) Double longitude,
                           @AuthenticationPrincipal CurrentUser currentUser,
                           RedirectAttributes redirectAttributes) {
        try {
            var stop = deliveryService.postpone(currentUser.getId(), stopId,
                    newDeliveryDate, note, latitude, longitude);
            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "Đã hoãn đơn của %s sang ngày %s. Hệ thống đã tạo lại đơn ở ngày mới.",
                    stop.getOrder().getCustomer().getName(),
                    newDeliveryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/my-route";
    }

    /** CN-06-01, CN-06-02 — tai xe bao su co tren duong. */
    @PostMapping("/my-route/incident")
    public String reportIncident(@RequestParam(required = false) IncidentType incidentType,
                                 @RequestParam(required = false) IncidentSeverity severity,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) Integer delayMinutes,
                                 @RequestParam(required = false) Double latitude,
                                 @RequestParam(required = false) Double longitude,
                                 @AuthenticationPrincipal CurrentUser currentUser,
                                 RedirectAttributes redirectAttributes) {
        try {
            IncidentService.ReportResult result = incidentService.report(currentUser.getId(),
                    incidentType, severity, description, delayMinutes, latitude, longitude);

            /*
             * Hai muc do bao hai cau khac han nhau, vi tai xe phai lam hai viec khac nhau tiep
             * theo: muc nhe thi cu di tiep, muc nang thi dung lai cho dieu phoi goi.
             */
            String message = result.needsDispatcher()
                    ? String.format("Đã báo sự cố. Điều phối viên đang xử lý %d điểm chưa giao. "
                            + "Bạn dừng lại chờ hướng dẫn, đừng tự đi tiếp.", result.affectedStopCount())
                    : String.format("Đã báo sự cố và lùi giờ dự kiến của %d điểm còn lại. "
                            + "Khách đã được thông báo, bạn cứ tiếp tục giao.", result.affectedStopCount());
            redirectAttributes.addFlashAttribute("successMessage", message);

        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/driver/my-route";
    }

    // ==================== Ham rieng ====================

    /**
     * Do het thu can hien ra model.
     *
     * Tinh san currentStop, so diem da xong va quang duong con lai o day chu khong de file
     * .html tu tinh: bieu thuc Thymeleaf khong go loi duoc, ma ba con so nay deu de sai.
     */
    private void fillModel(CurrentUser currentUser, Model model) {
        model.addAttribute("needsDriverJs", true);

        Optional<Route> route = deliveryService.todayRouteOf(currentUser.getId());
        model.addAttribute("route", route.orElse(null));

        // Tai khoan co vai tro DRIVER nhung chua duoc gan vao ho so tai xe nao — bao ro chu
        // khong de man hinh trong khong ai hieu tai sao
        model.addAttribute("driverProfileMissing",
                deliveryService.driverOf(currentUser.getId()).isEmpty());

        if (route.isEmpty()) {
            return;
        }

        model.addAttribute("currentStop", deliveryService.currentStopOf(route.get()).orElse(null));
        model.addAttribute("finishedStopCount", deliveryService.finishedStopCount(route.get()));
        model.addAttribute("remainingMeters", deliveryService.remainingMeters(route.get()));

        // CN-05-08: danh sach ly do that bai co san, tai xe khong duoc go tu do (BR05-06)
        model.addAttribute("failureReasons", FailureReason.values());

        // CN-06-02: loai su co va muc do, cung nguyen tac chon tu danh sach (BR06-01, BR06-03)
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("incidentSeverities", IncidentSeverity.values());

        // CN-05-09: hoan som nhat la ngay mai, va o chon ngay khong cho chon qua khu (BR05-07)
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1));
        model.addAttribute("today", LocalDate.now());

        // CN-05-10: chay xong ca tuyen thi hien man hinh tong ket cuoi ngay
        model.addAttribute("summary", deliveryService.summaryOf(route.get()));
    }
}
