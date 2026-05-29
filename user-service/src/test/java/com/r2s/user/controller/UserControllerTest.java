package com.r2s.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.response.ResponseBuilder;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.service.management.UserManagementService;
import com.r2s.user.service.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController test sau khi refactor SOLID:
 * - Inject 2 service interface RIENG (UserProfileService + UserManagementService) - ISP
 * - ResponseBuilder cho consistent ApiResponse format
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController tests (post-SOLID refactor)")
class UserControllerTest {

    @Mock private UserProfileService userProfileService;
    @Mock private UserManagementService userManagementService;

    private UserController userController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userController = new UserController(
                userProfileService, userManagementService, new ResponseBuilder());
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    @DisplayName("TC051 - GET /users/me happy case returns 200 with ApiResponse wrapper")
    void getMyProfile_HappyCase_Returns200() throws Exception {
        UserResponse user = new UserResponse();
        user.setUsername("john");
        user.setFullName("John Doe");
        user.setEmail("john@test.com");
        user.setRole("ROLE_USER");

        when(userProfileService.getUserByUsername("john")).thenReturn(user);

        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "john", null, java.util.Collections.emptyList());

        mockMvc.perform(get("/users/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("john"))
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"));
    }

    @Test
    @DisplayName("TC054 - GET /users happy case returns 200 with list inside ApiResponse")
    void getAllUsers_Returns200() throws Exception {
        UserResponse u1 = new UserResponse();
        u1.setUsername("john");
        UserResponse u2 = new UserResponse();
        u2.setUsername("jane");

        when(userProfileService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].username").value("john"))
                .andExpect(jsonPath("$.data[1].username").value("jane"));
    }
}
