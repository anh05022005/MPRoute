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
 * Phan quyen theo ma tran trong docs/01-nghiep-vu.md: quan tri vien toan quyen,
 * dieu phoi vien CHI XEM.
 *
 * Cach dat: annotation tren lop cho ca hai vai tro, cac method sua du lieu dat lai
 * @PreAuthorize rieng chi cho ADMIN. Luu y annotation tren METHOD **thay the** chu
 * khong cong don voi annotation tren LOP — nen dong tren method la chot cuoi cung.
 *
 * SecurityConfig cung da chan /admin/** o muc duong dan. Giu ca hai lop la co y (CBR-15):
 * sua nham mot ben thi ben con lai van giu.
 */
@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class ProductController {

    private static final int PAGE_SIZE = 15;

    private final ProductService productService;

    /**
     * Dua danh sach trang thai vao model cua MOI trang trong Controller nay.
     * Nho vay ca o loc tren danh sach lan o chon trong form deu dung chung mot nguon,
     * va template khong phai goi thang vao lop Java bang cu phap T(...).
     */
    @ModelAttribute("statuses")
    public RecordStatus[] statuses() {
        return RecordStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) RecordStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        var pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("code").ascending());
        Page<?> pageResult = productService.search(keyword, status, pageable);

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/products";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductForm());
        return "admin/product-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", ProductForm.from(productService.getById(id)));
        return "admin/product-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") ProductForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        // BR01-02 — kiem trung ma. Chi kiem khi o ma da qua duoc cac rang buoc khac,
        // khong thi de trong ma cung bao "ma da ton tai" thi rat kho hieu
        if (!binding.hasFieldErrors("code")
                && productService.isCodeTaken(form.getCode().trim(), form.getId())) {
            binding.rejectValue("code", "duplicateCode", "Mã sản phẩm này đã tồn tại");
        }

        if (binding.hasErrors()) {
            return "admin/product-form";
        }

        boolean isNew = (form.getId() == null);
        productService.save(form);

        redirectAttributes.addFlashAttribute("successMessage",
                isNew ? "Đã thêm sản phẩm mới." : "Lưu dữ liệu thành công.");
        return "redirect:/admin/products";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RecordStatus newStatus = productService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã chuyển sản phẩm sang trạng thái: " + newStatus.getDisplayName() + ".");
        return "redirect:/admin/products";
    }
}
