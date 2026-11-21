package com.lifelink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Role-based access rules. CSRF protection stays ON (Thymeleaf adds the token to every th:action form).
 * Passwords are hashed with BCrypt. Fine-grained checks (e.g. "only the owner of a request") are repeated in the services.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/register/**", "/faq", "/css/**", "/js/**",
                                "/images/**", "/favicon.ico", "/error", "/error/**").permitAll()
                        // REST API: public statistics are open, everything else under /api is staff/admin only
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "STAFF")
                        // administration
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // staff + admin only modules
                        .requestMatchers("/donors/**", "/inventory/**", "/reports/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/requests/*/status", "/requests/*/matches").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/camps/new", "/camps/*/edit", "/camps/*/toggle", "/camps/*/registrations")
                        .hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/appointments/*/mark").hasAnyRole("ADMIN", "STAFF")
                        // donor only
                        .requestMatchers("/donor/**", "/camps/*/book", "/appointments").hasRole("DONOR")
                        .requestMatchers("/appointments/*/cancel").hasAnyRole("DONOR", "ADMIN", "STAFF")
                        // recipient only
                        .requestMatchers("/requests/new", "/requests/*/edit", "/requests/*/cancel").hasRole("RECIPIENT")
                        // everything else needs a login (dashboard, request details, camps list, notifications, help)
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(e -> e.accessDeniedPage("/error/403"))
                .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()));
        return http.build();
    }
}
