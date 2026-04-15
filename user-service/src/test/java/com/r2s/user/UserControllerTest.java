package com.r2s.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.Role;
import com.r2s.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserResponse createMockResponse(String username, Role role) {
        UserResponse res = new UserResponse();
        res.setUsername(username);
        res.setRole(role.name());
        res.setEmail(username + "@example.com");
        res.setFullName(username + " Name");
        return res;
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllUsers_shouldReturnListOfUsers() throws Exception {
        List<UserResponse> mockUsers = List.of(
                createMockResponse("admin", Role.ROLE_ADMIN),
                createMockResponse("john", Role.ROLE_USER)
        );

        when(userService.getAllUsers()).thenReturn(mockUsers);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @WithMockUser(username = "john", roles = {"USER"})
    void getMyProfile_shouldReturnUserProfile() throws Exception {
        UserResponse mockResponse = createMockResponse("john", Role.ROLE_USER);

        when(userService.getUserByUsername("john")).thenReturn(mockResponse);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk());

        verify(userService).getUserByUsername("john");
    }

    @Test
    @WithMockUser(username = "john", roles = {"USER"})
    void updateMyProfile_shouldUpdateUser() throws Exception {
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("Updated Name");
        updateRequest.setEmail("updated@example.com");

        UserResponse updated = createMockResponse("john", Role.ROLE_USER);
        updated.setFullName("Updated Name");

        when(userService.updateUser(eq("john"), any(UpdateUserRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq("john"), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_shouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser("john");

        mockMvc.perform(delete("/users/john"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser("john");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getAllUsers_shouldReturn403IfNotAdmin() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_shouldReturn401IfNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }
}