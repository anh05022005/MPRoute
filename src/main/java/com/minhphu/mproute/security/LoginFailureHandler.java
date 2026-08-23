package com.minhphu.mproute.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Chay khi dang nhap that bai.
 *
 * Hai viec: dem so lan sai (de khoa tam 15 phut) va chon thong bao hien ra.
 *
 * Khong dung setDefaultFailureUrl cua SimpleUrlAuthenticationFailureHandler vi
 * lop nay la bean dung chung cho moi request — ghi de mot truong cua no la
 * hai nguoi dang nhap cung luc co the doc nham dia chi cua nhau.
 */
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    // Truong da gan gia tri san nen @RequiredArgsConstructor bo qua, khong dua vao constructor
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String username = request.getParameter("username");

        // Tai khoan dang bi khoa: Spring kiem tra khoa TRUOC khi so mat khau,
        // nen lan nay khong phai la mot lan go sai mat khau, khong dem vao bo dem
        if (exception instanceof LockedException) {
            redirectStrategy.sendRedirect(request, response, "/login?locked");
            return;
        }

        boolean justLocked = loginAttemptService.recordFailure(username);

        // Lan sai thu 5 vua lam khoa tai khoan thi bao MSG-13 luon,
        // khong de nguoi dung go them mot lan nua moi biet minh bi khoa
        redirectStrategy.sendRedirect(request, response, justLocked ? "/login?locked" : "/login?error");
    }
}
