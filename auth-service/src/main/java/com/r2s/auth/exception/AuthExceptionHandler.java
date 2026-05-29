package com.r2s.auth.exception;

import com.r2s.core.exception.DuplicateUsernameException;
import com.r2s.core.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler chuyen biet cho auth-service.
 *
 * <p>Su dung {@link ApiResponse} de tra response format thong nhat
 * voi cac endpoint khac (consistent voi controller dung ResponseBuilder).
 *
 * <p>Khi nao class nay duoc goi vs ApiResponseAuthenticationEntryPoint?
 * - @ExceptionHandler nay catch exception THROWN TU CONTROLLER (vd: BadCredentials
 *   tu PasswordAuthenticationStrategy goi qua AuthServiceImpl).
 * - {@link com.r2s.auth.security.ApiResponseAuthenticationEntryPoint} catch exception
 *   tu SPRING SECURITY FILTER CHAIN (vd: JWT token invalid).
 *
 * <p>Ca 2 deu tra ApiResponse format → consistent.
 */
@RestControllerAdvice
@Slf4j
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUsername(DuplicateUsernameException ex) {
        log.warn("Duplicate username: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(Exception ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }
}
