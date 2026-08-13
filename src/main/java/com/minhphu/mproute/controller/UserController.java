package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.UserForm;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.UserStatus;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SCR-08 — Danh muc tai khoan (CN-01-07).
 *
 * Chi quan tri vien vao duoc: ma tran phan quyen trong docs/01-nghiep-vu.md ghi
 * "Quan ly tai khoan: Quan tri toan quyen, ba vai tro con lai khong co quyen nao".
 */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private static final int SO_DONG_MOI_TRANG = 15;

    private final UserService userService;

    @ModelAttribute("cacVaiTro")
    public Role[] cacVaiTro() {
        return Role.values();
    }

    @ModelAttribute("cacTrangThai")
    public UserStatus[] cacTrangThai() {
        return UserStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) UserStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        var pageable = PageRequest.of(page, SO_DONG_MOI_TRANG, Sort.by("username").ascending());
        Page<User> trang = userService.search(keyword, role, status, pageable);

        model.addAttribute("trang", trang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        return "admin/users";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new UserForm());
        return "admin/user-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", UserForm.from(userService.getById(id)));
        return "admin/user-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") UserForm form,
                       BindingResult binding,
                       @AuthenticationPrincipal CurrentUser nguoiDangDangNhap,
                       RedirectAttributes redirectAttributes) {

        boolean themMoi = (form.getId() == null);

        /*
         * Bon phep kiem duoi day deu chay o PHIA MAY CHU (CBR-15).
         * Giao dien da chan san bang thuoc tinh required va bang viec an bot lua chon,
         * nhung den duoc day nghia la co nguoi tu gui du lieu len bang cong cu ngoai trinh duyet.
         */

        // 1. Ten dang nhap trung. Cot username dat unique: khong chan o day thi MySQL
        //    nem loi trung khoa va nguoi dung nhan duoc trang 500 thay vi mot dong loi tu te
        if (form.getUsername() != null && !form.getUsername().isBlank()
                && userService.isUsernameTaken(form.getUsername(), form.getId())) {
            binding.rejectValue("username", "trungTen", "Tên đăng nhập này đã có người dùng");
        }

        // 2. Them moi thi bat buoc co mat khau. Rang buoc nay phu thuoc vao viec dang
        //    them hay dang sua nen khong dat bang annotation tren UserForm duoc
        if (themMoi && (form.getPassword() == null || form.getPassword().isBlank())) {
            binding.rejectValue("password", "thieuMatKhau", "Vui lòng nhập mật khẩu cho tài khoản mới");
        }

        // 3. Go lai mat khau lan hai de tu bat loi go nham — nguoi dung khong nhin thay
        //    minh dang go gi, ma go nham thi lan sau khong dang nhap duoc
        if (form.getPassword() != null && !form.getPassword().isBlank()
                && !form.getPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "khongKhop", "Hai lần nhập mật khẩu không giống nhau");
        }

        /*
         * 4. Khong cho tu ha vai tro cua chinh minh.
         *
         * Cung voi phep chan tu khoa chinh minh o toggleStatus(), hai rao nay bao dam
         * he thong LUON con it nhat mot quan tri vien dang hoat dong: nguoi vua bam nut
         * chac chan la ADMIN va chac chan van con ACTIVE sau thao tac. Khong co hai rao do
         * thi mot cu bam nhe co the khoa het duong vao phan quan tri, phai sua thang trong CSDL.
         */
        if (!themMoi && form.getId().equals(nguoiDangDangNhap.getId()) && form.getRole() != Role.ADMIN) {
            binding.rejectValue("role", "tuHaQuyen",
                    "Không thể tự đổi vai trò của chính mình. Nhờ một quản trị viên khác thực hiện");
        }

        if (binding.hasErrors()) {
            return "admin/user-form";
        }

        userService.save(form);

        redirectAttributes.addFlashAttribute("thongBao",
                themMoi ? "Đã thêm tài khoản mới." : "Lưu dữ liệu thành công.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id,
                               @AuthenticationPrincipal CurrentUser nguoiDangDangNhap,
                               RedirectAttributes redirectAttributes) {

        // Tu khoa tai khoan cua chinh minh la tu dang xuat va khong vao lai duoc nua
        if (id.equals(nguoiDangDangNhap.getId())) {
            redirectAttributes.addFlashAttribute("loi",
                    "Không thể tự khóa tài khoản của chính mình.");
            return "redirect:/admin/users";
        }

        UserStatus moi = userService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("thongBao",
                "Đã chuyển tài khoản sang trạng thái: " + moi.getDisplayName() + ".");
        return "redirect:/admin/users";
    }

    /** Go khoa tam 15 phut do dang nhap sai 5 lan, khong dung den mat khau (CN-00-03). */
    @PostMapping("/{id}/clear-lock")
    public String clearTemporaryLock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.clearTemporaryLock(id);
        redirectAttributes.addFlashAttribute("thongBao",
                "Đã gỡ khóa tạm. Tài khoản đăng nhập lại được ngay.");
        return "redirect:/admin/users";
    }
}
