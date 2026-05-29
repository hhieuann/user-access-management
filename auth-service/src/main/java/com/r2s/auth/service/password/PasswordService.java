package com.r2s.auth.service.password;

/**
 * Interface chuyen biet cho cac password operations.
 *
 * <p>Tach rieng theo ISP de:
 * - AuthenticationService chi can goi matches() khi login
 * - RegistrationService chi can goi encodePassword() khi register
 * - Khong service nao "biet" la dung BCrypt/Argon2/etc (DIP)
 */
public interface PasswordService {

    /**
     * Encode (hash) raw password thanh string an toan luu DB.
     */
    String encodePassword(String rawPassword);

    /**
     * Kiem tra raw password co khop voi encoded password khong.
     *
     * @return true neu khop
     */
    boolean matches(String rawPassword, String encodedPassword);
}
