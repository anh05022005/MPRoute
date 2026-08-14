package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.SettingsForm;
import com.minhphu.mproute.entity.SystemParameter;
import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * SCR-09 — Tham so he thong (CN-01-08).
 *
 * Chi quan tri vien, khong cho dieu phoi vien xem — day la cac so anh huong thang
 * toi ket qua thuat toan. SecurityConfig da chan /admin/settings/** o muc URL,
 * @PreAuthorize o day la lop thu hai.
 */
@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SettingsController {

    private final SystemParameterService systemParameterService;

    /** Do vao o chon cua tham so DEFAULT_ALGORITHM. */
    @ModelAttribute("cacThuatToan")
    public AlgorithmType[] cacThuatToan() {
        return AlgorithmType.values();
    }

    @GetMapping
    public String edit(Model model) {
        model.addAttribute("form",
                SettingsForm.from(systemParameterService.findAllInDisplayOrder()));
        return "admin/settings";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("form") SettingsForm form,
                       BindingResult binding,
                       RedirectAttributes redirectAttributes) {

        /*
         * Moi ma tham so co kieu rieng nen khong dung Bean Validation duoc,
         * phai goi ham kiem theo tung ma. Loi gan vao dung chi so dong de
         * o nhap nao sai thi vien do sang mau do.
         */
        List<SettingsForm.Item> cacDong = form.getItems();
        for (int i = 0; i < cacDong.size(); i++) {
            SettingsForm.Item dong = cacDong.get(i);
            String loi = systemParameterService.kiemGiaTri(dong.getCode(), dong.getValue());
            if (loi != null) {
                binding.rejectValue("items[" + i + "].value", "thamSoSai", loi);
            }
        }

        if (binding.hasErrors()) {
            // Mo ta lay lai tu CSDL, khong dung gia tri trinh duyet gui len
            napLaiMoTa(form);
            return "admin/settings";
        }

        Map<String, String> giaTriMoi = new HashMap<>();
        for (SettingsForm.Item dong : cacDong) {
            giaTriMoi.put(dong.getCode(), dong.getValue());
        }
        int soSua = systemParameterService.updateValues(giaTriMoi);

        redirectAttributes.addFlashAttribute("thongBao", (soSua == 0)
                ? "Không có tham số nào thay đổi."
                : "Đã lưu " + soSua + " tham số.");
        return "redirect:/admin/settings";
    }

    private void napLaiMoTa(SettingsForm form) {
        Map<String, String> moTa = new HashMap<>();
        for (SystemParameter thamSo : systemParameterService.findAllInDisplayOrder()) {
            moTa.put(thamSo.getCode(), thamSo.getDescription());
        }
        for (SettingsForm.Item dong : form.getItems()) {
            dong.setDescription(moTa.get(dong.getCode()));
        }
    }
}
