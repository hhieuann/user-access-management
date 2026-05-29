package com.r2s.auth.strategy;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.service.password.PasswordService;
import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Strategy mac dinh: xac thuc bang username/password.
 *
 * <p>Khi them OAuth/SAML/2FA sau nay, them class moi implements
 * AuthenticationStrategy KHONG can sua class nay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {

    public static final String TYPE = "password";

    private final UserRepository userRepository;
    private final PasswordService passwordService;     // ← DIP
    private final JwtUtil jwtUtil;

    @Override
    public boolean supports(String authenticationType) {
        // Mac dinh "password" hoac khi authenticationType = null (legacy clients)
        return authenticationType == null || TYPE.equalsIgnoreCase(authenticationType);
    }

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        log.info("Authenticating via password strategy: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + request.getUsername()));

        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        log.info("User authenticated: {}", user.getUsername());
        return new AuthResponse(token);
    }
}
