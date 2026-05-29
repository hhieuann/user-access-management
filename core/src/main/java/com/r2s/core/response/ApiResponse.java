package com.r2s.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for all endpoints.
 *
 * <p>Format thong nhat cho moi response (success/error):
 * <pre>
 * {
 *   "success": true,
 *   "data":    {...},
 *   "message": "Login successful",
 *   "timestamp": "2026-05-21T14:30:00"
 * }
 * </pre>
 *
 * @param <T> kieu data
 */
@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** True neu thanh cong, false neu loi. */
    private boolean success;

    /** Payload chinh cua response (co the null neu loi). */
    private T data;

    /** Message giai thich (success message hoac error message). */
    private String message;

    /** Thoi diem server tao response. */
    private LocalDateTime timestamp;

    /** Factory: success response co data + message. */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Factory: success response chi co data, message default. */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success");
    }

    /** Factory: error response. */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(null)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
