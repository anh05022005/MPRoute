package com.minhphu.mproute.controller;

import com.minhphu.mproute.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Trang chu va trang bao khong du quyen.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    /**
     * Trang chao sau khi dang nhap (CN-00-01).
     * Noi dung doi theo vai trong — menu do layout.html tu an hien bang the sec:authorize.
     */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);
        return "home";
    }

    /**
     * Trang 403 (CN-00-04). SecurityConfig chuyen tiep vao day khi nguoi dung
     * da dang nhap nhung khong du quyen.
     *
     * @ResponseStatus de ma tra ve dung la 403 chu khong phai 200 — tieu chi
     * nghiem thu ghi ro "tra ve trang 403", hoi dong co the mo tab Network de kiem.
     */
    @GetMapping("/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser != null) {
            log.warn("Tai khoan {} (vai tro {}) truy cap chuc nang khong duoc phep",
                    currentUser.getUsername(), currentUser.getRole());
        }
        return "error/403";
    }
}
