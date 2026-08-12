package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiep vu lien quan den tai khoan nguoi dung.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Doi mat khau cho chinh nguoi dang dang nhap (CN-00-05).
     *
     * Van phai kiem lai mat khau hien tai du nguoi dung da dang nhap roi:
     * de may khong khoa man hinh thi nguoi khac ngoi vao doi mat khau duoc ngay.
     *
     * @throws IllegalArgumentException khi mat khau hien tai khong dung
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        // matches() bam currentPassword roi so voi chuoi trong DB.
        // Khong bao gio so sanh bang equals vi trong DB la chuoi da bam
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Tai khoan {} da doi mat khau", username);
    }
}
