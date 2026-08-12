package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Dung khi dang nhap: Spring Security tra cuu tai khoan theo ten dang nhap
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // Do danh sach tai khoan vao o chon "Tai khoan dang nhap" cua man hinh tai xe (CN-01-03)
    List<User> findByRoleOrderByUsernameAsc(Role role);
}
