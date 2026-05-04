package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doThrow;

import com.r2s.user.kafka.UserDeletedEventProducer;

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
    @DisplayName("TC016 - DeleteUser: Happy case - delete existing user")
    void deleteUser_HappyCase_DeletesUser() {
        // Given
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));
        doNothing().when(userRepository).deleteByUsername("john");

        // When
        userService.deleteUser("john");

        // Then
        verify(userRepository, times(1)).deleteByUsername("john");
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

    // ==================== TC032-TC033: GET USER BY USERNAME ====================

    @Test
    @DisplayName("TC032 - GetByUsername: Edge case - username null throws exception")
    void getUserByUsername_WhenNull_ThrowsException() {
        // Given
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class,
                () -> userService.getUserByUsername(null));
    }

    @Test
    @DisplayName("TC033 - GetByUsername: Edge case - username empty throws exception")
    void getUserByUsername_WhenEmpty_ThrowsException() {
        // Given
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class,
                () -> userService.getUserByUsername(""));
    }

    // ==================== TC034-TC038: UPDATE USER ====================

    @Test
    @DisplayName("TC034 - UpdateUser: Edge case - invalid email format")
    void updateUser_WhenInvalidEmailFormat_ProcessesAtServiceLayer() {
        // Given
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("John");
        req.setEmail("abc"); // Invalid format

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When - Service không validate format, @Email annotation ở DTO sẽ chặn ở Controller
        UserResponse result = userService.updateUser("john", req);

        // Then
        assertNotNull(result);
        // Validation thực tế xảy ra ở @Valid của Controller (test ở Phần 3)
    }

    @Test
    @DisplayName("TC035 - UpdateUser: Worst case - email already exists throws DB exception")
    void updateUser_WhenEmailExists_ThrowsException() {
        // Given
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("John");
        req.setEmail("existing@test.com");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "Email already exists"
                ));

        // When & Then
        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> userService.updateUser("john", req)
        );
    }

    @Test
    @DisplayName("TC036 - UpdateUser: Security - DTO does not expose role field")
    void updateUser_DtoDoesNotExposeRole() throws NoSuchFieldException {
        // Verify DTO design: UpdateUserRequest không có field 'role'
        // Đây là defense-in-depth: dù attacker gửi role trong JSON, Spring không bind vào DTO
        Class<UpdateUserRequest> clazz = UpdateUserRequest.class;
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

        boolean hasRoleField = false;
        for (java.lang.reflect.Field f : fields) {
            if (f.getName().equalsIgnoreCase("role")) {
                hasRoleField = true;
                break;
            }
        }

        assertFalse(hasRoleField, "UpdateUserRequest must NOT have role field for security");
    }

    @Test
    @DisplayName("TC037 - UpdateUser: Security - DTO does not expose password field")
    void updateUser_DtoDoesNotExposePassword() {
        // Verify DTO design: UpdateUserRequest không có field 'password'
        Class<UpdateUserRequest> clazz = UpdateUserRequest.class;
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

        boolean hasPasswordField = false;
        for (java.lang.reflect.Field f : fields) {
            if (f.getName().equalsIgnoreCase("password")) {
                hasPasswordField = true;
                break;
            }
        }

        assertFalse(hasPasswordField, "UpdateUserRequest must NOT have password field for security");
    }

    @Test
    @DisplayName("TC038 - UpdateUser: Edge case - request null throws NPE")
    void updateUser_WhenRequestNull_ThrowsException() {
        // Given
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));

        // When & Then
        assertThrows(NullPointerException.class,
                () -> userService.updateUser("john", null));
    }

    // ==================== TC039-TC040: DELETE USER ====================

    @Test
    @DisplayName("TC039 - DeleteUser: Happy case - delete and publish event")
    void deleteUser_HappyCase_PublishesEvent() {
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
    @DisplayName("TC040 - DeleteUser: Worst case - publish event fails")
    void deleteUser_WhenPublishFails_ThrowsException() {
        // Given
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user1));
        doNothing().when(userRepository).deleteByUsername("john");
        doThrow(new RuntimeException("Kafka broker unavailable"))
                .when(userDeletedEventProducer)
                .sendUserDeletedEvent(any(com.r2s.core.event.UserDeletedEvent.class));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> userService.deleteUser("john"));
    }
}