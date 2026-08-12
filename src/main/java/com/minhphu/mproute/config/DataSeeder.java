package com.minhphu.mproute.config;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.UserStatus;
import com.minhphu.mproute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Tao san ba tai khoan cua ba vai tro khi khoi dong, de co cai ma dang nhap thu.
 *
 * Chay lai nhieu lan khong sao: da co tai khoan thi bo qua, khong ghi de,
 * nen doi mat khau xong khoi dong lai ung dung khong bi tra ve mat khau cu.
 *
 * Du lieu mau day du (san pham, xe, khach hang, don hang) lam o tuan 11
 * theo docs/08-du-lieu-mau.md, chua lam o day.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("admin", "admin123", "Nguyễn Văn Quản", Role.ADMIN);
        seedUser("dieuphoi", "dieuphoi123", "Trần Thị Điều Phối", Role.DISPATCHER);
        seedUser("taixe01", "taixe123", "Lê Văn Tài", Role.DRIVER);
    }

    private void seedUser(String username, String rawPassword, String fullName, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        // Bam BCrypt truoc khi luu. Tuyet doi khong luu van ban thuan (CBR-16)
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);

        userRepository.save(user);
        log.info("Da tao tai khoan mau: {} / {} (vai tro {})", username, rawPassword, role);
    }
}
