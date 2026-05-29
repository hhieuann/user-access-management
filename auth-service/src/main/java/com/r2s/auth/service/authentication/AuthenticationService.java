package com.r2s.auth.service.authentication;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;

/**
 * Interface chuyen biet cho login flow.
 *
 * <p>Tach rieng theo nguyen tac ISP - client chi can login (mobile app,
 * frontend) khong bi buoc phu thuoc vao register/assignRole.
 */
public interface AuthenticationService {

    /**
     * Xac thuc user voi credential.
     *
     * @return AuthResponse chua JWT token neu thanh cong
     * @throws org.springframework.security.authentication.BadCredentialsException neu sai password
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException neu user khong ton tai
     */
    AuthResponse login(LoginRequest request);
}
