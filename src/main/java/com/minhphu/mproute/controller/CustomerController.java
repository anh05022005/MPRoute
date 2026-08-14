package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.CustomerForm;
import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.enums.PaymentMethod;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.service.CustomerService;
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
 * SCR-06 — Danh muc khach hang (CN-01-04).
 *
 * Quan tri vien toan quyen, dieu phoi vien chi xem — xem ghi chu o ProductController.
 */
@Controller
@RequestMapping("/admin/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class CustomerController {

    private static final int SO_DONG_MOI_TRANG = 15;

    private final CustomerService customerService;

    /** Dung chung cho o loc tren danh sach va o chon trang thai trong form. */
    @ModelAttribute("cacTrangThai")
    public RecordStatus[] cacTrangThai() {
        return RecordStatus.values();
    }

    @ModelAttribute("cacHinhThucThanhToan")
    public PaymentMethod[] cacHinhThucThanhToan() {
        return PaymentMethod.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) RecordStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        var pageable = PageRequest.of(page, SO_DONG_MOI_TRANG, Sort.by("name").ascending());
        Page<Customer> trang = customerService.search(keyword, status, pageable);

        model.addAttribute("trang", trang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/customers";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new CustomerForm());
        return "admin/customer-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", CustomerForm.from(customerService.getById(id)));
        return "admin/customer-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") CustomerForm form,
                       BindingResult binding,
                       RedirectAttributes redirectAttributes) {

        if (binding.hasErrors()) {
            return "admin/customer-form";
        }

        boolean themMoi = (form.getId() == null);

        /*
         * Trung so dien thoai chi CANH BAO chu khong chan: cot phone khong dat unique
         * trong CSDL (docs/02 chi danh index de tra cuu) vi thuc te co hai cua hang
         * cung mot chu dung chung mot so. Nhung trung so lam trang tra cuu cong khai
         * /track tra ve nhieu khach khac nhau, nen van bao cho nguoi nhap biet.
         */
        boolean trungSoDienThoai = customerService.isPhoneTaken(form.getPhone(), form.getId());

        customerService.save(form);

        redirectAttributes.addFlashAttribute("thongBao",
                themMoi ? "Đã thêm khách hàng mới." : "Lưu dữ liệu thành công.");
        if (trungSoDienThoai) {
            redirectAttributes.addFlashAttribute("loi",
                    "Lưu ý: số điện thoại này đã dùng cho một khách hàng khác. "
                            + "Dữ liệu vẫn được lưu, nhưng trang tra cứu công khai sẽ khó phân biệt.");
        }
        return "redirect:/admin/customers";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RecordStatus moi = customerService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("thongBao",
                "Đã chuyển khách hàng sang trạng thái: " + moi.getDisplayName() + ".");
        return "redirect:/admin/customers";
    }
}
