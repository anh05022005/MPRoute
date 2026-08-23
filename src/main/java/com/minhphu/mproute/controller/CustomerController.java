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

    private static final int PAGE_SIZE = 15;

    private final CustomerService customerService;

    /** Dung chung cho o loc tren danh sach va o chon trang thai trong form. */
    @ModelAttribute("statuses")
    public RecordStatus[] statuses() {
        return RecordStatus.values();
    }

    @ModelAttribute("paymentMethods")
    public PaymentMethod[] paymentMethods() {
        return PaymentMethod.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) RecordStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        var pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("name").ascending());
        Page<Customer> pageResult = customerService.search(keyword, status, pageable);

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/customers";
    }

    /*
     * Bao layout.html nap Leaflet cho man hinh nay (CN-01-05). Dat o ca ba cho tra ve
     * form: quen mot cho thi luc do ban do khong hien, ma o nhap toa do van trong.
     */
    private static final String CO_BAN_DO = "needsMap";

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new CustomerForm());
        model.addAttribute(CO_BAN_DO, true);
        return "admin/customer-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", CustomerForm.from(customerService.getById(id)));
        model.addAttribute(CO_BAN_DO, true);
        return "admin/customer-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") CustomerForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (binding.hasErrors()) {
            model.addAttribute(CO_BAN_DO, true);
            return "admin/customer-form";
        }

        boolean isNew = (form.getId() == null);

        /*
         * Trung so dien thoai chi CANH BAO chu khong chan: cot phone khong dat unique
         * trong CSDL (docs/02 chi danh index de tra cuu) vi thuc te co hai cua hang
         * cung mot chu dung chung mot so. Nhung trung so lam trang tra cuu cong khai
         * /track tra ve nhieu khach khac nhau, nen van bao cho nguoi nhap biet.
         */
        boolean phoneTaken = customerService.isPhoneTaken(form.getPhone(), form.getId());

        customerService.save(form);

        redirectAttributes.addFlashAttribute("successMessage",
                isNew ? "Đã thêm khách hàng mới." : "Lưu dữ liệu thành công.");
        if (phoneTaken) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lưu ý: số điện thoại này đã dùng cho một khách hàng khác. "
                            + "Dữ liệu vẫn được lưu, nhưng trang tra cứu công khai sẽ khó phân biệt.");
        }
        return "redirect:/admin/customers";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RecordStatus newStatus = customerService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã chuyển khách hàng sang trạng thái: " + newStatus.getDisplayName() + ".");
        return "redirect:/admin/customers";
    }
}
