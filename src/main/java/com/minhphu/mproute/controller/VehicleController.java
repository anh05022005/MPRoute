package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.VehicleForm;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.service.VehicleService;
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
 * SCR-03 — Danh muc phuong tien (CN-01-02).
 */
@Controller
@RequestMapping("/admin/vehicles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class VehicleController {

    private static final int SO_DONG_MOI_TRANG = 15;

    private final VehicleService vehicleService;

    /** Bo loc tren danh sach: cho loc ca IN_USE de nhin duoc xe nao dang chay. */
    @ModelAttribute("cacTrangThai")
    public VehicleStatus[] cacTrangThai() {
        return VehicleStatus.values();
    }

    /**
     * O chon trong form: CHI hai trang thai nay.
     * IN_USE do he thong dat khi chot tuyen, khong nam trong danh sach chon (BR01-04).
     */
    @ModelAttribute("cacTrangThaiChonDuoc")
    public VehicleStatus[] cacTrangThaiChonDuoc() {
        return new VehicleStatus[]{VehicleStatus.AVAILABLE, VehicleStatus.MAINTENANCE};
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) VehicleStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        var pageable = PageRequest.of(page, SO_DONG_MOI_TRANG, Sort.by("licensePlate").ascending());
        Page<Vehicle> trang = vehicleService.search(keyword, status, pageable);

        model.addAttribute("trang", trang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/vehicles";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new VehicleForm());
        return "admin/vehicle-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", VehicleForm.from(vehicleService.getById(id)));
        return "admin/vehicle-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") VehicleForm form,
                       BindingResult binding,
                       RedirectAttributes redirectAttributes) {

        // BR01-04 — chan o phia may chu, khong chi an o chon tren giao dien (CBR-15).
        // Giao dien khong co lua chon nay, nen den duoc day nghia la co nguoi tu gui
        // du lieu len bang cong cu ngoai trinh duyet
        if (form.getStatus() == VehicleStatus.IN_USE) {
            binding.rejectValue("status", "khongChonTay",
                    "Trạng thái \"Đang chạy\" do hệ thống tự đặt khi chốt tuyến, không chọn tay được");
        }

        // BR01-03 — bien so khong duoc trung
        if (!binding.hasFieldErrors("licensePlate")
                && vehicleService.isLicensePlateTaken(form.getLicensePlate().trim(), form.getId())) {
            binding.rejectValue("licensePlate", "trungBienSo", "Biển số này đã có trong danh mục");
        }

        if (binding.hasErrors()) {
            return "admin/vehicle-form";
        }

        boolean themMoi = (form.getId() == null);
        vehicleService.save(form);

        redirectAttributes.addFlashAttribute("thongBao",
                themMoi ? "Đã thêm phương tiện mới." : "Lưu dữ liệu thành công.");
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            VehicleStatus moi = vehicleService.toggleMaintenance(id);
            redirectAttributes.addFlashAttribute("thongBao",
                    "Đã chuyển phương tiện sang trạng thái: " + moi.getDisplayName() + ".");
        } catch (IllegalStateException e) {
            // Xe dang chay tuyen — hien loi o vung thong bao chung cua layout
            redirectAttributes.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }
}
