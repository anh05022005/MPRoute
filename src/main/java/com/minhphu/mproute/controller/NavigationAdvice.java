package com.minhphu.mproute.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Dua duong dan dang mo vao moi trang de layout.html to dam muc menu tuong ung.
 *
 * Thymeleaf 3.1 da bo doi tuong #request khoi bieu thuc, template khong tu doc
 * duoc URL nua nen phai di qua model. Dat o @ControllerAdvice de khong Controller
 * nao phai tu nho them dong nay.
 */
@ControllerAdvice
public class NavigationAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        // Cat context path de so khop thang voi duong dan viet trong layout (/orders, /routing...)
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
