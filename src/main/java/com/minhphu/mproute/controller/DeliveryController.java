package com.minhphu.mproute.controller;

import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // ==================== Ham rieng ====================

    /**
     * Do het thu can hien ra model.
     *
     * Tinh san currentStop, so diem da xong va quang duong con lai o day chu khong de file
     * .html tu tinh: bieu thuc Thymeleaf khong go loi duoc, ma ba con so nay deu de sai.
     */
    private void fillModel(CurrentUser currentUser, Model model) {
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
    }
}
