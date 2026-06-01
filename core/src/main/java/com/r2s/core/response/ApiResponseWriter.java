package com.r2s.core.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Helper dung chung de ghi {@link ApiResponse} ra HttpServletResponse.
 *
 * <p><b>Tai sao can?</b>
 * Cac security filter (JwtFilter, RateLimitFilter) chay TRUOC DispatcherServlet
 * nen khong di qua @RestControllerAdvice (GlobalExceptionHandler). Neu tu viet
 * response thi de bi lech format. Helper nay dam bao moi filter deu tra
 * ApiResponse format thong nhat {success, data, message, timestamp}.
 *
 * <p>Dung chung cho ca auth-service va user-service → tranh lap logic (DRY).
 */
@Component
public class ApiResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Ghi ApiResponse.error(message) ra response voi HTTP status chi dinh.
     *
     * @param response   servlet response
     * @param status     HTTP status code (vd: 401, 429)
     * @param message    error message
     */
    public void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(message);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
