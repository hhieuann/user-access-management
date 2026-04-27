package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;
import com.r2s.user.kafka.UserDeletedEventProducer;
import com.r2s.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDeletedEventProducer userDeletedEventProducer;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("john");
        user1.setFullName("John Doe");
        user1.setEmail("john@test.com");
        user1.setRole(Role.ROLE_USER);

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("jane");
        user2.setFullName("Jane Doe");
        user2.setEmail("jane@test.com");
        user2.setRole(Role.ROLE_ADMIN);

        updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("John Updated");
        updateRequest.setEmail("john_new@test.com");
    }

    // ==================== GET ALL USERS ====================

    @Test
    @DisplayName("TC009 - GetAll: Happy case - returns list of users")
    void getAllUsers_HappyCase_ReturnsList() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // When
        List<UserResponse> result = userService.getAllUsers();

        // Then
        assertEquals(2, result.size());
        assertEquals("john", result.get(0).getUsername());
        assertEquals("jane", result.get(1).getUsername());
    }

    @Test
    @DisplayName("TC010 - GetAll: Edge case - empty list when no users")
    void getAllUsers_WhenNoUsers_ReturnsEmptyList() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of());

        // When
        List<UserResponse> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== GET BY USERNAME ====================

    @Test
    @DisplayName("TC011 - GetByUsername: Happy case - returns user")
    void getUserByUsername_HappyCase_ReturnsUser() {
        // Given
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));

        // When
        UserResponse result = userService.getUserByUsername("john");

        // Then
        assertNotNull(result);
        assertEquals("john", result.getUsername());
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    @DisplayName("TC012 - GetByUsername: Worst case - user not found")
    void getUserByUsername_WhenNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                UsernameNotFoundException.class,
                () -> userService.getUserByUsername("notexist")
        );
    }

    // ==================== UPDATE USER ====================

    @Test
    @DisplayName("TC013 - UpdateUser: Happy case - update fullName and email")
    void updateUser_HappyCase_UpdatesUser() {
        // Given
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        UserResponse result = userService.updateUser("john", updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("John Updated", result.getFullName());
        assertEquals("john_new@test.com", result.getEmail());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    @DisplayName("TC014 - UpdateUser: Edge case - only update fullName, email is null")
    void updateUser_WhenOnlyFullName_UpdatesOnlyFullName() {
        // Given
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("Only Name Updated");
        req.setEmail(null);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        UserResponse result = userService.updateUser("john", req);

        // Then
        assertEquals("Only Name Updated", result.getFullName());
        assertEquals("john@test.com", result.getEmail()); // email không đổi
    }

    @Test
    @DisplayName("TC015 - UpdateUser: Worst case - user not found")
    void updateUser_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                UsernameNotFoundException.class,
                () -> userService.updateUser("notexist", updateRequest)
        );
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== DELETE USER ====================

    @Test
    @DisplayName("TC016 - DeleteUser: Happy case - delete existing user and publish event")
    void deleteUser_HappyCase_DeletesUser() {
        // Given
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));
        doNothing().when(userRepository).deleteByUsername("john");

        // When
        userService.deleteUser("john");

        // Then
        verify(userRepository, times(1)).deleteByUsername("john");
        verify(userDeletedEventProducer, times(1))
                .sendUserDeletedEvent(any(com.r2s.core.event.UserDeletedEvent.class));
    }

    @Test
    @DisplayName("TC017 - DeleteUser: Worst case - user not found")
    void deleteUser_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                UsernameNotFoundException.class,
                () -> userService.deleteUser("notexist")
        );
        verify(userRepository, never()).deleteByUsername(anyString());
    }
}