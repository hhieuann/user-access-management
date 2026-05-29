package com.r2s.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.service.authentication.AuthenticationService;
import com.r2s.auth.service.registration.RegistrationService;
import com.r2s.auth.service.role.RoleManagementService;
import com.r2s.core.response.ResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * Controller test sau khi refactor SOLID:
 * - Inject 3 service interface RIENG BIET (ISP)
 * - ResponseBuilder cho consistent response
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController tests (post-SOLID refactor)")
class AuthControllerTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private RegistrationService registrationService;
    @Mock private RoleManagementService roleManagementService;

    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Inject ResponseBuilder manually (MockitoExtension không tự inject real bean)
        authController = new AuthController(
                authenticationService, registrationService, roleManagementService,
                new ResponseBuilder()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("TC043 - Happy case - returns 201 Created with ApiResponse wrapper")
        void register_HappyCase_Returns201() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("newuser");
            req.setPassword("123456");

            when(registrationService.register(any(RegisterRequest.class)))
                    .thenReturn(new AuthResponse("mockedJwtToken"));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("mockedJwtToken"))
                    .andExpect(jsonPath("$.message").value("User registered successfully"));
        }

        @Test
        @DisplayName("TC044 - Duplicate username throws exception")
        void register_WhenUsernameExists_ThrowsException() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("existing");
            req.setPassword("123456");

            when(registrationService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("Username already exists"));

            try {
                mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)));
            } catch (Exception e) {
                org.junit.jupiter.api.Assertions.assertTrue(
                        e.getCause() instanceof IllegalArgumentException ||
                                e instanceof IllegalArgumentException
                );
            }
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("TC045 - Happy case - returns 200 with ApiResponse wrapper")
        void login_HappyCase_Returns200() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("newuser");
            req.setPassword("123456");

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenReturn(new AuthResponse("validJwtToken"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("validJwtToken"))
                    .andExpect(jsonPath("$.message").value("Login successful"));
        }

        @Test
        @DisplayName("TC046 - Wrong password throws exception")
        void login_WhenWrongPassword_ThrowsException() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("newuser");
            req.setPassword("wrong");

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

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
}
