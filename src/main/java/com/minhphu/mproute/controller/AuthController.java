package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.ChangePasswordForm;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Dang nhap, dang xuat, doi mat khau (CN-00-03, CN-00-05).
 *
 * Luu y: khong co method nao xu ly POST /login. Viec kiem tra ten dang nhap va
 * mat khau do bo loc cua Spring Security lam truoc khi request di toi Controller.
 * O day chi tra ve giao dien.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("form") ChangePasswordForm form,
                                 BindingResult binding,
                                 @AuthenticationPrincipal CurrentUser currentUser,
                                 RedirectAttributes redirectAttributes) {

        // Hai o mat khau moi khong khop nhau — kiem o day chu khong bang annotation,
        // vi annotation kiem tung truong rieng le, khong so sanh duoc hai truong voi nhau
        if (!binding.hasFieldErrors("confirmPassword")
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "passwordMismatch", "Mật khẩu nhập lại không khớp");
        }

        if (binding.hasErrors()) {
            return "change-password";
        }

        try {
            userService.changePassword(currentUser.getUsername(),
                    form.getCurrentPassword(), form.getNewPassword());
        } catch (IllegalArgumentException ex) {
            // Sai mat khau hien tai: hien loi ngay duoi o do, khong hien o dau trang
            binding.rejectValue("currentPassword", "incorrectPassword", ex.getMessage());
            return "change-password";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công.");
        return "redirect:/";
    }
}
