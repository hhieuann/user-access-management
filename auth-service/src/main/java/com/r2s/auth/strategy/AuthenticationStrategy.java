package com.r2s.auth.strategy;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;

/**
 * Strategy Pattern: dong goi thuat toan xac thuc thanh class rieng.
 *
 * <p>Cho phep them cach login moi (OAuth, SAML, 2FA...) bang cach them
 * 1 implementation moi - KHONG sua AuthServiceImpl (Open/Closed Principle).
 *
 * <p>Cach hoat dong:
 * 1. Moi strategy "dang ky" loai authentication no ho tro qua supports().
 * 2. AuthServiceImpl inject List<AuthenticationStrategy> (Spring tu inject moi bean).
 * 3. Khi login, AuthServiceImpl find strategy phu hop va goi authenticate().
 */
public interface AuthenticationStrategy {

    /**
     * Kiem tra strategy nay co ho tro authentication type khong.
     *
     * @param authenticationType vd: "password", "google-oauth", "saml"
     */
    boolean supports(String authenticationType);

    /**
     * Thuc hien xac thuc.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException neu sai credential
     */
    AuthResponse authenticate(LoginRequest request);
}
