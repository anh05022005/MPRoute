package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Dung khi dang nhap: Spring Security tra cuu tai khoan theo ten dang nhap
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // Do danh sach tai khoan vao o chon "Tai khoan dang nhap" cua man hinh tai xe (CN-01-03)
    List<User> findByRoleOrderByUsernameAsc(Role role);

    /**
     * Tim kiem cho man hinh danh muc tai khoan (CN-01-07).
     *
     * Ba dieu kien deu co dang ":x IS NULL OR ..." nen mot cau JPQL dung duoc cho
     * moi to hop bo loc, khoi phai noi chuoi SQL theo tung truong hop.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> search(@Param("keyword") String keyword,
                      @Param("role") Role role,
                      @Param("status") UserStatus status,
                      Pageable pageable);

    /** Dem tai khoan quan tri con hoat dong — dung de khong khoa het duong vao he thong. */
    long countByRoleAndStatus(Role role, UserStatus status);
}
