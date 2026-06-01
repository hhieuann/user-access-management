package com.r2s.core.exception;

import com.r2s.core.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho GlobalExceptionHandler - handler CHUNG cho toan he thong.
 *
 * <p>Sau khi xoa AuthExceptionHandler duplicate (theo review P2),
 * GlobalExceptionHandler la source of truth duy nhat. Test nay dam bao
 * duplicate username, bad credentials, access denied tra dung format/status.
 */
@DisplayName("GlobalExceptionHandler - centralized handler (source of truth)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("GEH01 - DuplicateUsernameException -> 409 Conflict + ApiResponse")
    void handleDuplicateUsername_Returns409() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleDuplicate(new DuplicateUsernameException("alice"));

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertNotNull(res.getBody());
        assertFalse(res.getBody().isSuccess());
        assertTrue(res.getBody().getMessage().contains("already exists"));
        assertNotNull(res.getBody().getTimestamp());
    }

    @Test
    @DisplayName("GEH02 - DuplicateEmailException -> 409 Conflict")
    void handleDuplicateEmail_Returns409() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleDuplicate(new DuplicateEmailException("a@b.com"));

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
    }

    @Test
    @DisplayName("GEH03 - BusinessException -> 400 Bad Request")
    void handleBusiness_Returns400() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleBusiness(new BusinessException("invalid state"));

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        assertEquals("invalid state", res.getBody().getMessage());
    }

    @Test
    @DisplayName("GEH04 - IllegalArgumentException -> 400 Bad Request")
    void handleIllegalArgument_Returns400() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleIllegalArgument(new IllegalArgumentException("bad arg"));

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        assertEquals("bad arg", res.getBody().getMessage());
    }

    @Test
    @DisplayName("GEH05 - BadCredentialsException -> 401 + generic message (no info leak)")
    void handleBadCredentials_Returns401() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleBadCredentials(new BadCredentialsException("wrong pw"));

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        // Generic - khong leak user ton tai hay khong
        assertEquals("Invalid username or password", res.getBody().getMessage());
    }

    @Test
    @DisplayName("GEH06 - UsernameNotFoundException -> 401 + generic message")
    void handleUsernameNotFound_Returns401() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleBadCredentials(new UsernameNotFoundException("not found"));

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals("Invalid username or password", res.getBody().getMessage());
    }

    @Test
    @DisplayName("GEH07 - AccessDeniedException -> 403 Forbidden")
    void handleAccessDenied_Returns403() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        assertEquals("Access denied", res.getBody().getMessage());
    }

    @Test
    @DisplayName("GEH08 - Generic Exception -> 500 + safe message (no stacktrace leak)")
    void handleAll_Returns500() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleAll(new RuntimeException("internal detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        // Khong leak chi tiet exception ra client
        assertEquals("An unexpected error occurred", res.getBody().getMessage());
    }
}
