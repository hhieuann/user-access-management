package com.r2s.auth.exception;

import com.r2s.core.exception.DuplicateUsernameException;
import com.r2s.core.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho AuthExceptionHandler sau khi refactor sang ApiResponse format.
 */
@DisplayName("AuthExceptionHandler - ApiResponse format")
class AuthExceptionHandlerTest {

    private AuthExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuthExceptionHandler();
    }

    @Test
    @DisplayName("TC059 - handleIllegalArgument: Trả 400 với ApiResponse format")
    void handleIllegalArgument_ReturnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad request");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Bad request", body.getMessage());
        assertNotNull(body.getTimestamp());
    }

    @Test
    @DisplayName("TC060 - handleBadCredentials (BadCredentialsException): Trả 401")
    void handleBadCredentials_WhenBadCredentialsException_ReturnsUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Wrong password");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Invalid username or password", body.getMessage());
    }

    @Test
    @DisplayName("TC061 - handleBadCredentials (UsernameNotFoundException): Trả 401 (no info leak)")
    void handleBadCredentials_WhenUsernameNotFoundException_ReturnsUnauthorized() {
        UsernameNotFoundException ex = new UsernameNotFoundException("User not found");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        // Generic message - tránh leak info user có tồn tại hay không
        assertEquals("Invalid username or password", body.getMessage());
    }

    @Test
    @DisplayName("TC062 - handleAccessDenied: Trả 403 Forbidden")
    void handleAccessDenied_ReturnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied to resource");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Access denied", body.getMessage());
    }

    @Test
    @DisplayName("TC063 - handleDuplicateUsername: Trả 409 Conflict")
    void handleDuplicateUsername_ReturnsConflict() {
        DuplicateUsernameException ex = new DuplicateUsernameException("alice");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicateUsername(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertTrue(body.getMessage().contains("already exists"));
    }
}
