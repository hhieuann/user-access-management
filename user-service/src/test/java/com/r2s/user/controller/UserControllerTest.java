package com.r2s.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserManagementService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    @DisplayName("TC051 - GET /users/me: Happy case - returns 200 with profile")
    void getMyProfile_HappyCase_Returns200() throws Exception {
        // Given
        UserResponse user = new UserResponse();
        user.setUsername("john");
        user.setFullName("John Doe");
        user.setEmail("john@test.com");
        user.setRole("ROLE_USER");

        when(userService.getUserByUsername("john")).thenReturn(user);

        // Mock Authentication để truyền qua method parameter
        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "john", null, java.util.Collections.emptyList()
                );

        // When & Then
        mockMvc.perform(get("/users/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    @DisplayName("TC054 - GET /users: Happy case - returns 200 with list")
    void getAllUsers_Returns200() throws Exception {
        // Given
        UserResponse u1 = new UserResponse();
        u1.setUsername("john");
        UserResponse u2 = new UserResponse();
        u2.setUsername("jane");

        when(userService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        // When & Then
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}