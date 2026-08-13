package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.UserForm;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.UserStatus;
import com.minhphu.mproute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiep vu lien quan den tai khoan nguoi dung (CN-00-05, CN-01-07).
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

    // ==================== Danh muc tai khoan (CN-01-07) ====================

    @Transactional(readOnly = true)
    public Page<User> search(String keyword, Role role, UserStatus status, Pageable pageable) {
        // O tim kiem de trong gui len chuoi rong chu khong phai null.
        // Doi ve null de cau JPQL bo qua dieu kien do
        String tuKhoa = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return userRepository.search(tuKhoa, role, status, pageable);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
    }

    /**
     * Cot username dat unique trong CSDL.
     *
     * excludeId dung khi dang SUA: tai khoan giu nguyen ten cua chinh no la binh thuong,
     * chi bao trung khi ten do thuoc ve mot tai khoan KHAC.
     */
    @Transactional(readOnly = true)
    public boolean isUsernameTaken(String username, Long excludeId) {
        return userRepository.findByUsername(username.trim().toLowerCase())
                .filter(taiKhoan -> !taiKhoan.getId().equals(excludeId))
                .isPresent();
    }

    @Transactional
    public User save(UserForm form) {
        // id null la them moi, co id la sua ban ghi dang co
        User user = (form.getId() == null) ? new User() : getById(form.getId());

        // Ha chu thuong de "Admin" va "admin" khong thanh hai tai khoan khac nhau
        user.setUsername(form.getUsername().trim().toLowerCase());
        user.setFullName(form.getFullName().trim());
        user.setEmail(chuoiHoacNull(form.getEmail()));
        user.setRole(form.getRole());
        user.setStatus(form.getStatus());

        /*
         * O mat khau de trong khi SUA nghia la giu nguyen mat khau cu.
         *
         * Neu khong co nhanh if nay thi moi lan quan tri vien sua ho ten hay doi vai tro
         * la mat khau bi bam lai tu chuoi rong — nguoi dung dang dung binh thuong bong dung
         * khong dang nhap duoc nua, ma nhin man hinh khong thay dau hieu gi.
         */
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
            // Dat lai mat khau thi go luon khoa tam, khong bat nguoi dung ngoi cho het 15 phut
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            log.info("Dat lai mat khau cho tai khoan {}", user.getUsername());
        }

        User daLuu = userRepository.save(user);
        log.info("Luu tai khoan {} - vai tro {}", daLuu.getUsername(), daLuu.getRole());
        return daLuu;
    }

    /**
     * Doi giua ACTIVE va LOCKED — day la khoa VINH VIEN do quan tri vien dat.
     *
     * CBR-17: khong co ham xoa cung. Tai khoan da tao don, da chay tuyen ma xoa di
     * thi lich su trang thai mat thong tin ai lam viec gi. Nhan vien nghi viec thi khoa.
     */
    @Transactional
    public UserStatus toggleStatus(Long id) {
        User user = getById(id);

        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.LOCKED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
            /*
             * Mo khoa vinh vien thi phai go ca khoa TAM di.
             *
             * CustomUserDetailsService.isLocked() kiem hai dieu kien noi bang HOAC.
             * Neu chi dat status = ACTIVE ma con locked_until trong tuong lai thi quan tri vien
             * bam "Mo khoa", man hinh bao thanh cong, nhung nguoi dung van khong vao duoc —
             * loi kieu nay rat kho doan ra khi dang demo.
             */
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        userRepository.save(user);
        log.info("Tai khoan {} chuyen sang trang thai {}", user.getUsername(), user.getStatus());
        return user.getStatus();
    }

    /**
     * Go rieng khoa tam 15 phut do go sai mat khau 5 lan (CN-00-03), giu nguyen
     * trang thai ACTIVE. Dung khi nguoi dung goi dien bao "toi khong dang nhap duoc"
     * ma quan tri vien khong muon doi mat khau cua ho.
     */
    @Transactional
    public void clearTemporaryLock(Long id) {
        User user = getById(id);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("Da go khoa tam cho tai khoan {}", user.getUsername());
    }

    // O nhap khong bat buoc de trong gui len chuoi rong, luu vao DB nen la NULL cho gon
    private String chuoiHoacNull(String giaTri) {
        return (giaTri == null || giaTri.isBlank()) ? null : giaTri.trim();
    }
}
