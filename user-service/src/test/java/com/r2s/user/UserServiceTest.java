package com.r2s.user;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;
import com.r2s.user.repository.UserRepository;
import com.r2s.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // Tạo user mẫu dùng chung cho các test
    private User createTestUser() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setFullName("John Doe");
        user.setRole(Role.ROLE_USER);
        return user;
    }

    // === TEST getAllUsers() ===
    @Test
    void getAllUsers_shouldReturnListOfUserResponses() {
        List<User> mockUsers = List.of(
                createTestUser()
        );

        Mockito.when(userRepository.findAll()).thenReturn(mockUsers);

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("john", result.get(0).getUsername());

        verify(userRepository, times(1)).findAll();
    }

    // === TEST getUserByUsername() - success ===
    @Test
    void getUserByUsername_shouldReturnUserResponse() {
        User mockUser = createTestUser();

        Mockito.when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));

        UserResponse result = userService.getUserByUsername("john");

        assertEquals("john", result.getUsername());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByUsername("john");
    }

    // === TEST getUserByUsername() - not found ===
    @Test
    void getUserByUsername_shouldThrowExceptionIfNotFound() {
        Mockito.when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.getUserByUsername("missing");
        });

        Mockito.verify(userRepository, Mockito.times(1))
                .findByUsername("missing");
    }

    // === TEST updateUser() ===
    @Test
    void updateUser_shouldUpdateAndReturnUserResponse() {
        User mockUser = createTestUser();

        UpdateUserRequest update = new UpdateUserRequest();
        update.setFullName("New Name");
        update.setEmail("new@example.com");

        Mockito.when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        UserResponse result = userService.updateUser("john", update);

        assertEquals("New Name", result.getFullName());
        assertEquals("new@example.com", result.getEmail());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByUsername("john");
        Mockito.verify(userRepository, Mockito.times(1))
                .save(mockUser);
    }

    // === TEST deleteUser() - success ===
    @Test
    void deleteUser_shouldDeleteIfExists() {
        User mockUser = createTestUser();

        Mockito.when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));

        userService.deleteUser("john");

        Mockito.verify(userRepository, Mockito.times(1))
                .findByUsername("john");
        Mockito.verify(userRepository, Mockito.times(1))
                .delete(mockUser);
    }

    // === TEST deleteUser() - not found ===
    @Test
    void deleteUser_shouldThrowIfUserNotFound() {
        Mockito.when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.deleteUser("missing"));

        Mockito.verify(userRepository, Mockito.times(1))
                .findByUsername("missing");
        Mockito.verify(userRepository, never())
                .delete(Mockito.any());
    }
}