package com.r2s.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom AuthenticationEntryPoint trả ApiResponse format thay vì Spring default.
 *
 * <p><b>Tại sao cần?</b>
 * Khi Spring Security throw {@link AuthenticationException} (BadCredentials,
 * UsernameNotFound...) từ filter chain, {@code ExceptionTranslationFilter}
 * intercept TRƯỚC {@code @RestControllerAdvice} → response không đi qua
 * GlobalExceptionHandler → trả Spring default error format
 * (timestamp/status/error/path) thay vì ApiResponse wrapper.
 *
 * <p>Custom entry point này được gọi bởi ExceptionTranslationFilter cho 401 cases
 * → đảm bảo response luôn theo format thống nhất.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Authentication failed: {} - {}",
                request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Generic message - khong leak thong tin (user not found vs wrong password)
        ApiResponse<Void> body = ApiResponse.error("Invalid username or password");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
