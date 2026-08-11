package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Dung khi dang nhap: Spring Security tra cuu tai khoan theo ten dang nhap
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
