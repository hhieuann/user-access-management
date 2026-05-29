package com.r2s.core.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Utility class de build response thong nhat cho moi controller.
 *
 * <p>Su dung de tranh moi controller tu setup ResponseEntity rieng → DRY.
 * Moi response deu wrap trong {@link ApiResponse} → consistent format.
 */
@Component
public class ResponseBuilder {

    /**
     * Build success response (200 OK) voi data + message.
     */
    public <T> ResponseEntity<ApiResponse<T>> buildSuccessResponse(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * Build success response (200 OK) chi co data, message default = "Success".
     */
    public <T> ResponseEntity<ApiResponse<T>> buildSuccessResponse(T data) {
        return buildSuccessResponse(data, "Success");
    }

    /**
     * Build created response (201 CREATED) voi data + message.
     */
    public <T> ResponseEntity<ApiResponse<T>> buildCreatedResponse(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, message));
    }

    /**
     * Build no content response (204).
     */
    public ResponseEntity<Void> buildNoContentResponse() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Build bad request response (400) voi error message.
     */
    public <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /**
     * Build not found response (404).
     */
    public <T> ResponseEntity<ApiResponse<T>> buildNotFoundResponse(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    /**
     * Build unauthorized response (401).
     */
    public <T> ResponseEntity<ApiResponse<T>> buildUnauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    /**
     * Build forbidden response (403).
     */
    public <T> ResponseEntity<ApiResponse<T>> buildForbiddenResponse(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }

    /**
     * Build conflict response (409) - cho duplicate resource.
     */
    public <T> ResponseEntity<ApiResponse<T>> buildConflictResponse(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }
}
