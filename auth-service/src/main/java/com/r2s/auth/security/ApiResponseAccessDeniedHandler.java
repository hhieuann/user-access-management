package com.r2s.auth.security;

import com.r2s.core.response.ApiResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom AccessDeniedHandler trả ApiResponse format cho 403 Forbidden.
 *
 * <p><b>Tại sao cần?</b>
 * Khi {@code @PreAuthorize("hasRole('ADMIN')")} fail, exception được handle bởi
 * AccessDeniedHandler - không qua @RestControllerAdvice. Default handler trả
 * Spring error format, không consistent với ApiResponse.
 *
 * <p>Dùng chung {@link ApiResponseWriter} với JwtFilter/RateLimitFilter (DRY).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiResponseAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiResponseWriter apiResponseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied: {} - {}",
                request.getRequestURI(), accessDeniedException.getMessage());

        apiResponseWriter.writeError(
                response, HttpStatus.FORBIDDEN.value(), "Access denied");
    }
}
