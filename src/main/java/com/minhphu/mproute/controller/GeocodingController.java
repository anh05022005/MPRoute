package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.GeocodingResult;
import com.minhphu.mproute.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tra ket qua tim toa do dang JSON cho nut "Tim toa do" (CN-01-05).
 *
 * @RestController chu khong phai @Controller: cho nay tra du lieu cho JavaScript,
 * khong tra ten view.
 *
 * Mo cho quan tri vien VA dieu phoi vien, khong mo rong hon: hai vai tro nay deu co
 * man hinh phai xac dinh toa do — quan tri vien o form khach hang va form kho,
 * dieu phoi vien o form tao don khi ghi de dia chi giao cho rieng mot don (BR02-08).
 * Van dong voi tai xe va nguoi chua dang nhap, vi mo rong hon muc can thiet la bien
 * may chu cua do an thanh mot cai cong goi Nominatim mien phi cho nguoi khac dung (CBR-15).
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping("/admin/geocode")
    public List<GeocodingResult> search(@RequestParam("q") String keyword) {
        return geocodingService.search(keyword);
    }
}
