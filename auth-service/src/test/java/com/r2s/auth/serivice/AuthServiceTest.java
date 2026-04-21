package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;
import com.r2s.auth.kafka.UserEventProducer;
import com.r2s.auth.repository.UserRepository;
import com.r2s.core.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("123456");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("newuser");
        loginRequest.setPassword("123456");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("newuser");
        existingUser.setPassword("encodedPassword");
        existingUser.setRole(Role.ROLE_USER);
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("TC001 - Register: Happy case - new username should succeed")
    void register_HappyCase_ReturnsToken() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(jwtUtil.generateToken("newuser")).thenReturn("mockedToken");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("mockedToken", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userEventProducer, times(1)).sendUserRegisteredEvent(any());
    }

    @Test
    @DisplayName("TC002 - Register: Worst case - existing username should throw exception")
    void register_WhenUsernameExists_ThrowsException() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));

        // When & Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
        verify(userEventProducer, never()).sendUserRegisteredEvent(any());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("TC003 - Login: Happy case - correct credentials should return token")
    void login_HappyCase_ReturnsToken() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("newuser")).thenReturn("mockedToken");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("mockedToken", response.getToken());
    }

    @Test
    @DisplayName("TC004 - Login: Worst case - non-existent user should throw exception")
    void login_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                UsernameNotFoundException.class,
                () -> authService.login(loginRequest)
        );
    }

    @Test
    @DisplayName("TC005 - Login: Worst case - wrong password should throw exception")
    void login_WhenWrongPassword_ThrowsException() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
    }

    // ==================== ASSIGN ROLE TESTS ====================

    @Test
    @DisplayName("TC006 - AssignRole: Happy case - assign ADMIN to existing user")
    void assignRole_HappyCase_UpdatesRole() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        authService.assignRole("newuser", Role.ROLE_ADMIN);

        // Then
        assertEquals(Role.ROLE_ADMIN, existingUser.getRole());
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    @DisplayName("TC007 - AssignRole: Worst case - non-existent user should throw exception")
    void assignRole_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                UsernameNotFoundException.class,
                () -> authService.assignRole("newuser", Role.ROLE_ADMIN)
        );
        verify(userRepository, never()).save(any(User.class));
    }
}