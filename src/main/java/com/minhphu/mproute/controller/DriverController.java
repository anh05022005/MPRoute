package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.DriverForm;
import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SCR-05 — Danh muc tai xe (CN-01-03).
 *
 * Quan tri vien toan quyen, dieu phoi vien chi xem — xem ghi chu o ProductController.
 */
@Controller
@RequestMapping("/admin/drivers")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class DriverController {

    private static final int PAGE_SIZE = 15;

    private final DriverService driverService;

    /** Dung chung cho o loc tren danh sach va o chon trang thai trong form. */
    @ModelAttribute("statuses")
    public RecordStatus[] statuses() {
        return RecordStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) RecordStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        var pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("fullName").ascending());
        Page<Driver> pageResult = driverService.search(keyword, status, pageable);

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/drivers";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new DriverForm());
        loadSelectOptions(model, null);
        return "admin/driver-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", DriverForm.from(driverService.getById(id)));
        loadSelectOptions(model, id);
        return "admin/driver-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") DriverForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        /*
         * Hai phep kiem duoi day deu la kiem o PHIA MAY CHU (CBR-15).
         * O chon tren giao dien da loc san, nen den duoc day nghia la co nguoi
         * tu gui du lieu len bang cong cu ngoai trinh duyet.
         */
        if (form.getUserId() != null) {
            // Cot user_id dat unique: khong chan o day thi MySQL nem loi trung khoa, ra trang 500
            if (driverService.isAccountTaken(form.getUserId(), form.getId())) {
                binding.rejectValue("userId", "duplicateUsername",
                        "Tài khoản này đã gán cho tài xế khác");
            } else if (!driverService.isDriverAccount(form.getUserId())) {
                binding.rejectValue("userId", "invalidRole",
                        "Tài khoản được chọn không tồn tại hoặc không phải vai trò Tài xế");
            }
        }

        if (binding.hasErrors()) {
            // Ve lai form thi phai do lai hai o chon, khong thi chung rong tron
            loadSelectOptions(model, form.getId());
            return "admin/driver-form";
        }

        boolean isNew = (form.getId() == null);
        driverService.save(form);

        redirectAttributes.addFlashAttribute("successMessage",
                isNew ? "Đã thêm tài xế mới." : "Lưu dữ liệu thành công.");
        return "redirect:/admin/drivers";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RecordStatus newStatus = driverService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã chuyển tài xế sang trạng thái: " + newStatus.getDisplayName() + ".");
        return "redirect:/admin/drivers";
    }

    // Hai o chon phu thuoc du lieu trong CSDL nen phai nap lai moi lan tra ve form
    private void loadSelectOptions(Model model, Long driverId) {
        model.addAttribute("accounts", driverService.selectableAccounts(driverId));
        model.addAttribute("vehicles", driverService.selectableVehicles());
    }
}
