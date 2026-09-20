package com.minhphu.mproute.controller;

import com.minhphu.mproute.service.TrackingBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * SCR-10 — man hinh theo doi tien do giao hang trong ngay (CN-04-11).
 *
 * <p>Khong nham voi {@link TrackingController} o duong dan /track: lop kia la trang tra cuu
 * cong khai cho khach hang, khong can dang nhap va phai che giau gan het thong tin. Lop nay
 * danh cho dieu phoi da dang nhap, hien day du.
 *
 * <p>Quyen kiem o CA HAI noi: SecurityConfig da chan /tracking/** cho ADMIN va DISPATCHER, va
 * them mot lop @PreAuthorize ngay tren lop nay. An muc menu bang sec:authorize la chua du —
 * hoi dong se go thang duong dan vao thanh dia chi de thu (CBR-15).
 */
@Controller
@RequestMapping("/tracking")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class TrackingBoardController {

    private final TrackingBoardService trackingBoardService;

    /**
     * Khong truyen ngay thi lay hom nay — man hinh nay dung de nhin ngay dang chay, mo ra ma
     * phai chon ngay truoc moi thay gi la thua mot thao tac cho viec dung hang ngay.
     */
    @GetMapping
    public String board(@RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;

        model.addAttribute("board", trackingBoardService.board(day));
        model.addAttribute("date", day);

        return "tracking";
    }
}
