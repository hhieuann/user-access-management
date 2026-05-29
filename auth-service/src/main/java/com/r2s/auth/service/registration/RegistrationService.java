package com.r2s.auth.service.registration;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.RegisterRequest;

/**
 * Interface chuyen biet cho registration flow.
 *
 * <p>Tach rieng theo nguyen tac ISP - registration la flow doc lap voi login.
 */
public interface RegistrationService {

    /**
     * Dang ky user moi.
     *
     * @return AuthResponse chua JWT token cho user moi
     * @throws com.r2s.core.exception.DuplicateUsernameException neu username da ton tai
     */
    AuthResponse register(RegisterRequest request);
}
