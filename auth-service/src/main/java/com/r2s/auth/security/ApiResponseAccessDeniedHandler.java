package com.r2s.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom AccessDeniedHandler trả ApiResponse format cho 403 Forbidden.
 *
 * <p><b>Tại sao cần?</b>
 * Khi {@code @PreAuthorize("hasRole('ADMIN')")} fail, hoặc Spring Security
 * authorization layer reject request, exception duoc handle boi
 * AccessDeniedHandler - khong qua @RestControllerAdvice.
 * Default handler tra Spring error format, khong consistent voi ApiResponse.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiResponseAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied: {} - {}",
                request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.error("Access denied");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
