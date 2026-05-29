package com.r2s.auth.service.authentication;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.strategy.AuthenticationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation cua {@link AuthenticationService}.
 *
 * <p>Su dung Strategy Pattern: chon strategy phu hop voi authType cua request.
 * Hien tai chi co PasswordAuthenticationStrategy.
 *
 * <p>Khi them OAuth/SAML/2FA: them strategy moi, KHONG sua class nay (OCP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthenticationService {

    /** Spring tu inject TAT CA bean implements AuthenticationStrategy. */
    private final List<AuthenticationStrategy> strategies;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: user={}, authType={}",
                request.getUsername(), request.getAuthType());

        AuthenticationStrategy strategy = strategies.stream()
                .filter(s -> s.supports(request.getAuthType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No authentication strategy found for type: " + request.getAuthType()));

        return strategy.authenticate(request);
    }
}
