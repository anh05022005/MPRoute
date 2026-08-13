package com.minhphu.mproute.security;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.UserStatus;
import com.minhphu.mproute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cau noi giua bang users va Spring Security.
 *
 * Spring Security khong biet gi ve bang users cua do an. Moi lan co nguoi bam
 * dang nhap, no goi loadUserByUsername va cho nhan lai mot doi tuong UserDetails
 * chua mat khau da bam de tu so sanh. Lop nay lam dung viec do.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong co tai khoan: " + username));

        return new CurrentUser(user, isLocked(user));
    }

    /**
     * Tai khoan bi khoa trong hai truong hop khac nhau:
     *   1. Quan tri vien khoa vinh vien  -> users.status = LOCKED
     *   2. Sai mat khau 5 lan            -> users.locked_until la thoi diem trong tuong lai
     *
     * Khoa tam het han thi khong can xoa locked_until, chi can so sanh voi gio hien tai.
     */
    private boolean isLocked(User user) {
        return user.getStatus() == UserStatus.LOCKED || user.isTemporarilyLocked();
    }
}
