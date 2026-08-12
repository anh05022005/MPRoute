package com.minhphu.mproute.security;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Nguoi dung dang dang nhap, do Spring Security giu trong SecurityContext.
 *
 * Ke thua lop User cua Spring Security va gan them ba truong cua rieng do an:
 * id, ho ten, vai tro. Nho vay giao dien goi thang duoc principal.fullName
 * ma khong phai truy van lai bang users moi lan ve trang.
 *
 * Luu y: lop User o day co HAI nghia khac nhau —
 *   com.minhphu.mproute.entity.User                    la ban ghi trong bang users
 *   org.springframework.security.core.userdetails.User la lop cua Spring Security
 * Vi trung ten nen lop cha phai viet day du duong dan.
 */
@Getter
public class CurrentUser extends org.springframework.security.core.userdetails.User {

    private final Long id;
    private final String fullName;
    private final Role role;

    public CurrentUser(User user, boolean locked) {
        super(
                user.getUsername(),
                user.getPassword(),
                true,       // enabled
                true,       // accountNonExpired
                true,       // credentialsNonExpired
                !locked,    // accountNonLocked - false thi Spring nem LockedException
                // Spring Security doi tien to ROLE_ thi hasRole("ADMIN") moi khop
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.role = user.getRole();
    }
}
