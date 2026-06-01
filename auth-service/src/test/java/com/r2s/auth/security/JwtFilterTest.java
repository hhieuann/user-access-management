package com.r2s.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.response.ApiResponseWriter;
import com.r2s.core.security.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    // Real helper de assert body JSON that.
    // findAndRegisterModules() de ObjectMapper ho tro LocalDateTime (JSR310),
    // giong ObjectMapper ma Spring Boot auto-config trong production.
    private final ApiResponseWriter apiResponseWriter =
            new ApiResponseWriter(new ObjectMapper().findAndRegisterModules());

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, userDetailsService, apiResponseWriter);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC047 - JwtFilter: No Authorization header - filter chain continues without auth")
    void doFilterInternal_NoAuthHeader_ContinuesWithoutAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        assertNotEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("TC048 - JwtFilter: Wrong prefix (not Bearer) - filter chain continues")
    void doFilterInternal_WrongPrefix_ContinuesWithoutAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("Authorization", "Token abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("TC049 - JwtFilter: Expired token - returns 401 with ApiResponse body")
    void doFilterInternal_ExpiredToken_Returns401() throws Exception {
        String expiredToken = "expired_token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.extractUsername(expiredToken))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Status 401 + KHONG goi filterChain tiep
        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // Verify body ApiResponse format (P1 - consistent response)
        String body = response.getContentAsString();
        assertTrue(body.contains("\"success\":false"), "Body phai co success=false");
        assertTrue(body.contains("\"message\":\"Invalid or expired token\""),
                "Body phai co message");
        assertTrue(body.contains("\"timestamp\""), "Body phai co timestamp");
        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    @DisplayName("TC050 - JwtFilter: Invalid signature token - returns 401 with ApiResponse body")
    void doFilterInternal_InvalidSignature_Returns401() throws Exception {
        String invalidToken = "invalid_signature_token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("Authorization", "Bearer " + invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.extractUsername(invalidToken))
                .thenThrow(new SignatureException("Invalid signature"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        String body = response.getContentAsString();
        assertTrue(body.contains("\"success\":false"));
        assertTrue(body.contains("\"timestamp\""));
    }
}
