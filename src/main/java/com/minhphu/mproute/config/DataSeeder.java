package com.minhphu.mproute.config;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.UserStatus;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Tao san nam tai khoan mau khi khoi dong, dung theo bang trong docs/08-du-lieu-mau.md.
 *
 * Chay lai nhieu lan khong sao: da co tai khoan thi bo qua, khong ghi de,
 * nen doi mat khau xong khoi dong lai ung dung khong bi tra ve mat khau cu.
 *
 * Vi sao phai du BA tai xe chu khong phai mot: kich ban demo buoc 12-14 dien
 * phuong an B — chen cac diem con lai cua xe hong vao mot tuyen KHAC dang chay.
 * Muon dung duoc tinh huong do thi phai co it nhat hai tai xe chay song song
 * trong cung mot ngay.
 *
 * Du lieu mau con lai (san pham, xe, khach hang, don hang) lam o tuan 11,
 * cung theo docs/08-du-lieu-mau.md.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemParameterService systemParameterService;

    @Override
    public void run(String... args) {
        // Chin tham so he thong theo docs/02 muc 16. Thieu chung thi tu tuan 8
        // man hinh phan tuyen khong co toc do, gio xuat phat hay he so quy doi de chay
        systemParameterService.seedDefaults();

        seedUser("admin", "admin123", "Nguyễn Thị Lan", Role.ADMIN);
        seedUser("dieuphoi", "dieuphoi123", "Trần Văn Minh", Role.DISPATCHER);
        // Ho ten ba tai xe phai TRUNG voi ho so trong danh muc tai xe (CN-01-03),
        // khong thi man hinh tai xe hien mot ten ma thanh tieu de hien ten khac
        seedUser("taixe01", "taixe123", "Nguyễn Văn Hùng", Role.DRIVER);
        seedUser("taixe02", "taixe123", "Lê Văn Nam", Role.DRIVER);
        seedUser("taixe03", "taixe123", "Phạm Văn Tú", Role.DRIVER);
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
