package com.minhphu.mproute.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Chay khi dang nhap thanh cong.
 *
 * Ke thua SavedRequestAwareAuthenticationSuccessHandler de giu duoc hanh vi:
 * ai dang go dia chi /orders ma bi day ve trang dang nhap thi sau khi dang nhap
 * xong se quay lai dung /orders, khong bi nem ve trang chu.
 *
 * Khong co dia chi nao duoc nho thi ve "/" — trang chu tu doi noi dung theo
 * vai tro cua nguoi dang nhap (xem HomeController).
 */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Xoa bo dem sai mat khau, neu khong thi lan sai truoc do van con tich luy
        loginAttemptService.recordSuccess(authentication.getName());

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
