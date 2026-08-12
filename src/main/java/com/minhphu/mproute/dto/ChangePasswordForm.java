package com.minhphu.mproute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Du lieu form doi mat khau (CN-00-05).
 *
 * Tach rieng khoi entity User: form nay nhan mat khau dang van ban thuan,
 * con entity chi chua chuoi da bam. Buoc chung vao mot lop la de nham lan.
 */
@Data
public class ChangePasswordForm {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải có từ 6 đến 50 ký tự")
    private String newPassword;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String confirmPassword;
}
