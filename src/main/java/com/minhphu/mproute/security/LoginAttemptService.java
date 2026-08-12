package com.minhphu.mproute.security;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Dem so lan dang nhap sai va khoa tam tai khoan (CN-00-03).
 *
 * Bo dem nam o cot users.failed_login_attempts chu khong nam trong bo nho,
 * de khoi dong lai ung dung khong lam mat trang thai khoa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;

    /**
     * Ghi nhan mot lan dang nhap sai.
     *
     * @return true neu lan sai nay lam tai khoan bi khoa tam
     */
    @Transactional
    public boolean recordFailure(String username) {
        // Go sai ten dang nhap thi khong co gi de dem. Cung khong bao loi ra ngoai,
        // vi bao "khong ton tai tai khoan nay" la tu tiet lo tai khoan nao co that.
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        int attempts = user.getFailedLoginAttempts() + 1;

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Dat lai bo dem ve 0 de sau khi het khoa nguoi dung lai co du 5 lan thu
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            userRepository.save(user);
            log.warn("Khoa tam tai khoan {} trong {} phut do sai mat khau {} lan lien tiep",
                    username, LOCK_MINUTES, MAX_FAILED_ATTEMPTS);
            return true;
        }

        user.setFailedLoginAttempts(attempts);
        userRepository.save(user);
        log.info("Tai khoan {} dang nhap sai lan thu {}/{}", username, attempts, MAX_FAILED_ATTEMPTS);
        return false;
    }

    /**
     * Dang nhap dung thi xoa sach bo dem va khoa tam.
     * Neu khong xoa, 4 lan sai hom truoc cong voi 1 lan sai hom sau la bi khoa oan.
     */
    @Transactional
    public void recordSuccess(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }
}
