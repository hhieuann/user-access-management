package com.r2s.auth.security;

import com.r2s.core.response.ApiResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom AuthenticationEntryPoint trả ApiResponse format thay vì Spring default.
 *
 * <p><b>Tại sao cần?</b>
 * Khi Spring Security throw {@link AuthenticationException} từ filter chain,
 * {@code ExceptionTranslationFilter} intercept TRƯỚC {@code @RestControllerAdvice}
 * → response không đi qua GlobalExceptionHandler → trả Spring default error format.
 * Custom entry point này đảm bảo response luôn theo ApiResponse format thống nhất.
 *
 * <p>Dùng chung {@link ApiResponseWriter} với JwtFilter/RateLimitFilter (DRY).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiResponseWriter apiResponseWriter;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Authentication failed: {} - {}",
                request.getRequestURI(), authException.getMessage());

        // Generic message - khong leak thong tin (user not found vs wrong password)
        apiResponseWriter.writeError(
                response, HttpStatus.UNAUTHORIZED.value(), "Invalid username or password");
    }
}
