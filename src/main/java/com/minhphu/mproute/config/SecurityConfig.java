package com.minhphu.mproute.config;

import com.minhphu.mproute.security.LoginFailureHandler;
import com.minhphu.mproute.security.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cau hinh dang nhap va phan quyen (CN-00-03, CN-00-04).
 *
 * Nguyen tac CBR-15: chan quyen o PHIA MAY CHU. An nut tren giao dien chi la
 * cho de nhin, hoi dong se go thang dia chi vao thanh dia chi de thu.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // bat @PreAuthorize de chan quyen o tung method service khi can
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;

    /**
     * BCrypt tu sinh chuoi ngau nhien (salt) rieng cho tung mat khau va nhung
     * luon vao ket qua, nen hai nguoi dat cung mat khau van ra hai chuoi bam khac nhau.
     * Chuoi bam dai 60 ky tu, khop voi length = 100 cua cot users.password (CBR-16).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Duong dan cong khai, dung theo docs/01-nghiep-vu.md
                        .requestMatchers("/login", "/track", "/track/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // Trang loi mac dinh cua Spring Boot, khong mo thi loi 500 bi lap vo han
                        .requestMatchers("/error").permitAll()

                        /*
                         * Ma tran phan quyen — docs/01-nghiep-vu.md
                         *
                         * Quan ly tai khoan va cau hinh tham so: CHI quan tri vien.
                         * Hai dong nay phai dat TRUOC dong /admin/** ben duoi — Spring Security
                         * xet cac matcher theo dung thu tu khai bao va dung lai o cai khop dau tien,
                         * dat sau thi khong bao gio toi luot.
                         */
                        .requestMatchers("/admin/users/**", "/admin/settings/**").hasRole("ADMIN")

                        /*
                         * Con lai la cac man hinh danh muc. Dieu phoi vien duoc vao nhung
                         * CHI XEM: quyen sua nam o @PreAuthorize tren tung method cua Controller,
                         * vi o day khong phan biet duoc GET danh sach voi GET man hinh them moi.
                         */
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "DISPATCHER")
                        // Tai xe chi co mot man hinh duy nhat cua rieng minh
                        .requestMatchers("/driver/**").hasRole("DRIVER")
                        // Don hang, phan tuyen, theo doi, su co: dieu phoi lam, quan tri xem
                        .requestMatchers("/orders/**", "/routing/**", "/tracking/**", "/incidents/**")
                        .hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/reports/**").hasAnyRole("ADMIN", "DISPATCHER")

                        // Con lai (trang chu, doi mat khau, trang 403) chi can dang nhap
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")            // trang dang nhap tu viet, khong dung trang mac dinh
                        .loginProcessingUrl("/login")   // dia chi form POST vao, Spring tu xu ly
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // Dang nhap roi nhung khong du quyen -> trang 403 tu viet,
                // khong phai trang trang hay loi 500 (tieu chi CN-00-04)
                .exceptionHandling(ex -> ex.accessDeniedPage("/403"));

        // KHONG tat CSRF. Spring Security bat san, moi form Thymeleaf tu chen the an
        // chua token. Tat di cho tien la mat diem phan bao mat.
        return http.build();
    }
}
