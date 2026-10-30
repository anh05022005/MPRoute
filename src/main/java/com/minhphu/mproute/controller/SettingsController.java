package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.SettingsForm;
import com.minhphu.mproute.entity.SystemParameter;
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
        List<SettingsForm.Item> rows = form.getItems();
        for (int i = 0; i < rows.size(); i++) {
            SettingsForm.Item row = rows.get(i);
            String error = systemParameterService.validateValue(row.getCode(), row.getValue());
            if (error != null) {
                binding.rejectValue("items[" + i + "].value", "invalidParameter", error);
            }
        }

        if (binding.hasErrors()) {
            // Mo ta lay lai tu CSDL, khong dung gia tri trinh duyet gui len
            reloadDescriptions(form);
            return "admin/settings";
        }

        Map<String, String> newValues = new HashMap<>();
        for (SettingsForm.Item row : rows) {
            newValues.put(row.getCode(), row.getValue());
        }
        int updatedCount = systemParameterService.updateValues(newValues);

        redirectAttributes.addFlashAttribute("successMessage", (updatedCount == 0)
                ? "Không có tham số nào thay đổi."
                : "Đã lưu " + updatedCount + " tham số.");
        return "redirect:/admin/settings";
    }

    private void reloadDescriptions(SettingsForm form) {
        Map<String, String> descriptions = new HashMap<>();
        for (SystemParameter parameter : systemParameterService.findAllInDisplayOrder()) {
            descriptions.put(parameter.getCode(), parameter.getDescription());
        }
        for (SettingsForm.Item row : form.getItems()) {
            row.setDescription(descriptions.get(row.getCode()));
        }
    }
}
