package com.r2s.auth.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthExceptionHandler Unit Tests")
class AuthExceptionHandlerTest {

    private AuthExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuthExceptionHandler();
    }

    @Test
    @DisplayName("TC059 - handleIllegalArgument: Trả về 400 Bad Request với message gốc")
    void handleIllegalArgument_ReturnsBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Username already exists");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Username already exists", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("TC060 - handleBadCredentials (BadCredentialsException): Trả về 401")
    void handleBadCredentials_WhenBadCredentialsException_ReturnsUnauthorized() {
        // Arrange
        BadCredentialsException ex = new BadCredentialsException("Wrong password");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(ex);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(401, body.get("status"));
        assertEquals("Invalid username or password", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("TC061 - handleBadCredentials (UsernameNotFoundException): Trả về 401")
    void handleBadCredentials_WhenUsernameNotFoundException_ReturnsUnauthorized() {
        // Arrange
        UsernameNotFoundException ex = new UsernameNotFoundException("User not found");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(ex);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(401, body.get("status"));
        // Note: message luôn là "Invalid username or password" để tránh leak info user có tồn tại hay không
        assertEquals("Invalid username or password", body.get("message"));
    }

    @Test
    @DisplayName("TC062 - handleAccessDenied: Trả về 403 Forbidden")
    void handleAccessDenied_ReturnsForbidden() {
        // Arrange
        AccessDeniedException ex = new AccessDeniedException("Access denied to resource");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.get("status"));
        assertEquals("Access denied", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("TC063 - handleIllegalArgument: Message null vẫn xử lý đúng")
    void handleIllegalArgument_WhenMessageNull_StillReturnsBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException();

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertNull(body.get("message"));
    }
}