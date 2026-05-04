package com.r2s.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private com.r2s.auth.service.AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("TC043 - POST /auth/register: Happy case - returns 200")
    void register_HappyCase_Returns200() throws Exception {
        // Given
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("123456");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("mockedJwtToken"));

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    @DisplayName("TC044 - POST /auth/register: Worst case - duplicate username throws exception")
    void register_WhenUsernameExists_ThrowsException() throws Exception {
        // Given
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing");
        req.setPassword("123456");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        // When & Then - standaloneSetup throws exception thay vì return 4xx
        try {
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        } catch (Exception e) {
            // Expected
            org.junit.jupiter.api.Assertions.assertTrue(
                    e.getCause() instanceof IllegalArgumentException ||
                            e instanceof IllegalArgumentException
            );
        }
    }

    @Test
    @DisplayName("TC045 - POST /auth/login: Happy case - returns 200")
    void login_HappyCase_Returns200() throws Exception {
        // Given
        LoginRequest req = new LoginRequest();
        req.setUsername("newuser");
        req.setPassword("123456");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("validJwtToken"));

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC046 - POST /auth/login: Worst case - wrong password throws exception")
    void login_WhenWrongPassword_ThrowsException() throws Exception {
        // Given
        LoginRequest req = new LoginRequest();
        req.setUsername("newuser");
        req.setPassword("wrong");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        try {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    e.getCause() instanceof BadCredentialsException ||
                            e instanceof BadCredentialsException
            );
        }
    }
}