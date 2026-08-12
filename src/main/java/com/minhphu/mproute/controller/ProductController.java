package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.ProductForm;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.service.ProductService;
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
 * SCR-02 — Danh muc san pham (CN-01-01).
 *
 * SecurityConfig da chan ca nhanh /admin/** cho vai tro ADMIN. Them @PreAuthorize
 * o day la lop chan thu hai: neu sau nay ai do sua duong dan trong SecurityConfig
 * thi van con mot lop giu (CBR-15).
 */
@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProductController {

    private static final int SO_DONG_MOI_TRANG = 15;

    private final ProductService productService;

    /**
     * Dua danh sach trang thai vao model cua MOI trang trong Controller nay.
     * Nho vay ca o loc tren danh sach lan o chon trong form deu dung chung mot nguon,
     * va template khong phai goi thang vao lop Java bang cu phap T(...).
     */
    @ModelAttribute("cacTrangThai")
    public RecordStatus[] cacTrangThai() {
        return RecordStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) RecordStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        var pageable = PageRequest.of(page, SO_DONG_MOI_TRANG, Sort.by("code").ascending());
        Page<?> trang = productService.search(keyword, status, pageable);

        model.addAttribute("trang", trang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/products";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductForm());
        return "admin/product-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", ProductForm.from(productService.getById(id)));
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") ProductForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        // BR01-02 — kiem trung ma. Chi kiem khi o ma da qua duoc cac rang buoc khac,
        // khong thi de trong ma cung bao "ma da ton tai" thi rat kho hieu
        if (!binding.hasFieldErrors("code")
                && productService.isCodeTaken(form.getCode().trim(), form.getId())) {
            binding.rejectValue("code", "trungMa", "Mã sản phẩm này đã tồn tại");
        }

        if (binding.hasErrors()) {
            return "admin/product-form";
        }

        boolean themMoi = (form.getId() == null);
        productService.save(form);

        redirectAttributes.addFlashAttribute("thongBao",
                themMoi ? "Đã thêm sản phẩm mới." : "Lưu dữ liệu thành công.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RecordStatus moi = productService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("thongBao",
                "Đã chuyển sản phẩm sang trạng thái: " + moi.getDisplayName() + ".");
        return "redirect:/admin/products";
    }
}
