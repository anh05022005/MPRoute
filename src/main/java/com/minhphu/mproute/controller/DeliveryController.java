package com.minhphu.mproute.controller;

import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.enums.FailureReason;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.DeliveryService;
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

    /** CN-05-01 — tuyen hom nay cua chinh tai xe dang dang nhap. */
    @GetMapping("/my-route")
    public String myRoute(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        fillModel(currentUser, model);
        return "driver/my-route";
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

        // CN-05-09: hoan som nhat la ngay mai, va o chon ngay khong cho chon qua khu (BR05-07)
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1));
        model.addAttribute("today", LocalDate.now());

        // CN-05-10: chay xong ca tuyen thi hien man hinh tong ket cuoi ngay
        model.addAttribute("summary", deliveryService.summaryOf(route.get()));
    }
}
