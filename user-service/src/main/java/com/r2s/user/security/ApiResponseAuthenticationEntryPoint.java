package com.r2s.user.security;

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
 * Custom AuthenticationEntryPoint cho user-service - tra ApiResponse format
 * cho 401 Unauthorized errors.
 *
 * <p>Dùng chung {@link ApiResponseWriter} với JwtFilter (DRY).
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
        log.warn("Authentication required: {}", request.getRequestURI());

        apiResponseWriter.writeError(
                response, HttpStatus.UNAUTHORIZED.value(), "Authentication required");
    }
}
