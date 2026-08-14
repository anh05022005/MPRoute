package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.DepotForm;
import com.minhphu.mproute.service.DepotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SCR-07 — Danh muc kho (CN-01-06).
 *
 * Duong dan de so it "/admin/depot" cho khop voi muc menu da co trong layout.html.
 * Quan tri vien toan quyen, dieu phoi vien chi xem — xem ghi chu o ProductController.
 *
 * Khong phan trang: thuc te chi co mot kho, them phan trang chi ton code.
 */
@Controller
@RequestMapping("/admin/depot")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class DepotController {

    private final DepotService depotService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("danhSach", depotService.findAll());
        return "admin/depots";
    }

    /*
     * Bao layout.html nap Leaflet cho man hinh nay (CN-01-05). Dat o ca ba cho tra ve
     * form: quen mot cho thi luc do ban do khong hien, ma o nhap toa do van trong.
     */
    private static final String CO_BAN_DO = "canBanDo";

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new DepotForm());
        model.addAttribute(CO_BAN_DO, true);
        return "admin/depot-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", DepotForm.from(depotService.getById(id)));
        model.addAttribute(CO_BAN_DO, true);
        return "admin/depot-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") DepotForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        /*
         * BR01-10 kiem o PHIA MAY CHU (CBR-15). O tich tren giao dien da bi khoa san
         * khi day la kho active duy nhat, nen den duoc day nghia la co nguoi tu gui
         * du lieu len bang cong cu ngoai trinh duyet.
         *
         * Bo tich kho active cuoi cung se lam he thong khong con diem c0 de phan tuyen.
         */
        if (!Boolean.TRUE.equals(form.getActive())
                && !depotService.hasAnotherActiveDepot(form.getId())) {
            binding.rejectValue("active", "khoCuoiCung",
                    "Phải luôn có một kho đang hoạt động. Muốn đổi kho chính thì "
                            + "đặt kho khác làm kho chính, kho này sẽ tự tắt.");
        }

        if (binding.hasErrors()) {
            model.addAttribute(CO_BAN_DO, true);
            return "admin/depot-form";
        }

        boolean themMoi = (form.getId() == null);
        depotService.save(form);

        redirectAttributes.addFlashAttribute("thongBao",
                themMoi ? "Đã thêm kho mới." : "Lưu dữ liệu thành công.");
        return "redirect:/admin/depot";
    }

    /**
     * Dat mot kho lam kho chinh.
     *
     * Khong co duong nguoc lai ("tat kho chinh"): doi kho la bat kho MOI len,
     * kho cu tu tat. Nho vay khong bao gio di qua trang thai khong con kho nao active.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/set-active")
    public String setActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        var kho = depotService.setActive(id);
        redirectAttributes.addFlashAttribute("thongBao",
                "Đã đặt \"" + kho.getName() + "\" làm kho chính. Các kho khác đã tự chuyển sang ngừng dùng.");
        return "redirect:/admin/depot";
    }
}
