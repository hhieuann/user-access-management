package com.r2s.auth.service.password;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementation cua {@link PasswordService} dung Spring Security PasswordEncoder.
 *
 * <p>BCrypt la implementation duoc inject (xem SecurityConfig).
 * Doi sang Argon2/Scrypt chi can sua bean PasswordEncoder, KHONG sua service. (DIP)
 */
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final PasswordEncoder passwordEncoder;  // ← DIP: phu thuoc abstraction

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
