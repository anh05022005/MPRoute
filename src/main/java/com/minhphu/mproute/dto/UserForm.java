package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Du lieu form them/sua tai khoan (CN-01-07).
 */
@Data
public class UserForm {

    private Long id;

    /*
     * Ten dang nhap chi cho chu thuong khong dau, chu so, dau cham va gach duoi.
     *
     * Vi sao chan chat: nguoi dung go ten dang nhap tren dien thoai (tai xe), co dau
     * tieng Viet hay chu hoa lan vao la go mai khong vao duoc ma khong hieu vi sao.
     * Service ha chu thuong truoc khi luu nen "Admin" va "admin" khong thanh hai tai khoan.
     *
     * Nhanh "^$" cho chuoi rong di qua de o bo trong chi hien mot dong loi cua @NotBlank,
     * khong hien chong hai dong cho cung mot o.
     */
    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự")
    @Pattern(regexp = "^$|^[a-z0-9._]{3,50}$",
            message = "Tên đăng nhập từ 3 ký tự, chỉ gồm chữ thường không dấu, chữ số, dấu chấm và gạch dưới")
    private String username;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    // Duoc phep de trong. Chi tai khoan nao can nhan thu moi phai co email
    @Email(message = "Địa chỉ email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotNull(message = "Vui lòng chọn vai trò")
    private Role role;

    private UserStatus status = UserStatus.ACTIVE;

    /*
     * Mat khau. Quy uoc cua man hinh nay:
     *   - Them moi : bat buoc nhap. Chan o UserController vi rang buoc nay phu thuoc
     *                vao viec dang them hay dang sua, mot annotation khong dien ta duoc
     *   - Sua      : de trong nghia la GIU NGUYEN mat khau cu. Co nhap thi la dat lai
     *
     * Gioi han 72 ky tu khong phai con so tuy tien: BCrypt chi bam 72 byte dau tien,
     * phan du bi bo qua am tham. Cho nhap dai hon la tao cam giac an toan gia.
     *
     * Man hinh nay khong hien mat khau cu vi trong CSDL chi co chuoi bam mot chieu,
     * khong co cach nao doc nguoc ra (CBR-16).
     */
    @Pattern(regexp = "^$|^.{6,72}$", message = "Mật khẩu phải từ 6 đến 72 ký tự")
    private String password;

    private String confirmPassword;

    public static UserForm from(User user) {
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setStatus(user.getStatus());
        // Co tinh KHONG do mat khau ra form: trong DB la chuoi bam, do ra cung vo nghia,
        // ma do chuoi bam ra HTML la tu lam lo du lieu nhay cam
        return form;
    }
}
