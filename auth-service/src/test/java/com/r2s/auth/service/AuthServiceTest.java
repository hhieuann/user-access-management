package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;
import com.r2s.auth.kafka.UserEventProducer;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.service.authentication.AuthServiceImpl;
import com.r2s.auth.service.password.PasswordService;
import com.r2s.auth.service.password.PasswordServiceImpl;
import com.r2s.auth.service.registration.RegistrationServiceImpl;
import com.r2s.auth.service.role.RoleManagementServiceImpl;
import com.r2s.auth.strategy.AuthenticationStrategy;
import com.r2s.auth.strategy.PasswordAuthenticationStrategy;
import com.r2s.auth.testdata.TestDataBuilder;
import com.r2s.core.exception.DuplicateUsernameException;
import com.r2s.core.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static com.r2s.auth.testdata.TestDataBuilder.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho cac auth services sau khi tach interface theo ISP.
 *
 * <p>3 @Nested class group test theo trach nhiem (SOLID-aware testing):
 * - RegistrationTests: test RegistrationServiceImpl
 * - AuthenticationTests: test AuthServiceImpl + PasswordAuthenticationStrategy
 * - RoleManagementTests: test RoleManagementServiceImpl
 *
 * <p>Su dung TestDataBuilder (Builder Pattern) de tao test data.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Auth services - SOLID refactored")
class AuthServiceTest {

    // ============================================================
    // 1. REGISTRATION TESTS
    // ============================================================
    @Nested
    @DisplayName("Registration flow")
    class RegistrationTests {

        @Mock UserRepository userRepository;
        @Mock PasswordService passwordService;   // ← Mock abstraction (DIP)
        @Mock JwtUtil jwtUtil;
        @Mock UserEventProducer userEventProducer;

        @InjectMocks RegistrationServiceImpl registrationService;

        @Test
        @DisplayName("TC001 - Register happy case returns token + persists encoded password")
        void register_HappyCase_ReturnsToken() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());
            when(passwordService.encodePassword(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(aUser());
            when(jwtUtil.generateToken(TEST_USERNAME)).thenReturn(TEST_TOKEN);

            AuthResponse response = registrationService.register(aRegisterRequest());

            assertNotNull(response);
            assertEquals(TEST_TOKEN, response.getToken());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertEquals(TEST_USERNAME, capturedUser.getUsername());
            assertEquals(TEST_ENCODED_PASSWORD, capturedUser.getPassword());
            assertEquals(Role.ROLE_USER, capturedUser.getRole());

            verify(userEventProducer, times(1)).sendUserRegisteredEvent(any());
        }

        @Test
        @DisplayName("TC002 - Duplicate username throws DuplicateUsernameException")
        void register_WhenUsernameExists_ThrowsDuplicateException() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(aUser()));

            DuplicateUsernameException ex = assertThrows(
                    DuplicateUsernameException.class,
                    () -> registrationService.register(aRegisterRequest())
            );
            assertTrue(ex.getMessage().contains("already exists"));
            verify(userRepository, never()).save(any(User.class));
            verify(userEventProducer, never()).sendUserRegisteredEvent(any());
        }

        @Test
        @DisplayName("TC020 - Register: username null processes at service layer")
        void register_WhenUsernameNull_HandledByDtoValidation() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(null);
            req.setPassword(TEST_PASSWORD);
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());
            when(passwordService.encodePassword(TEST_PASSWORD)).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(aUser());
            when(jwtUtil.generateToken(null)).thenReturn("token");

            AuthResponse response = registrationService.register(req);

            assertNotNull(response);
        }

        @Test
        @DisplayName("TC022 - Register: password null throws exception")
        void register_WhenPasswordNull_ThrowsException() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword(null);
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());
            when(passwordService.encodePassword(null))
                    .thenThrow(new IllegalArgumentException("Password cannot be null"));

            assertThrows(Exception.class, () -> registrationService.register(req));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("TC024 - Register: very long username (256 chars) processes")
        void register_WhenUsernameVeryLong_ProcessesAtServiceLayer() {
            String longUsername = "a".repeat(256);
            RegisterRequest req = aRegisterRequest(longUsername);
            when(userRepository.findByUsername(longUsername)).thenReturn(Optional.empty());
            when(passwordService.encodePassword(TEST_PASSWORD)).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(aUser());
            when(jwtUtil.generateToken(longUsername)).thenReturn("token");

            AuthResponse response = registrationService.register(req);

            assertNotNull(response);
        }
    }

    // ============================================================
    // 2. AUTHENTICATION (LOGIN) TESTS - Strategy Pattern
    // ============================================================
    @Nested
    @DisplayName("Authentication flow (Strategy Pattern)")
    class AuthenticationTests {

        @Mock UserRepository userRepository;
        @Mock PasswordService passwordService;
        @Mock JwtUtil jwtUtil;

        private AuthServiceImpl authService;
        private PasswordAuthenticationStrategy passwordStrategy;

        @BeforeEach
        void setUp() {
            passwordStrategy = new PasswordAuthenticationStrategy(
                    userRepository, passwordService, jwtUtil);
            authService = new AuthServiceImpl(List.of(passwordStrategy));
        }

        @Test
        @DisplayName("TC003 - Login happy case returns token")
        void login_HappyCase_ReturnsToken() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(aUser()));
            when(passwordService.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(TEST_USERNAME)).thenReturn(TEST_TOKEN);

            AuthResponse response = authService.login(aLoginRequest());

            assertNotNull(response);
            assertEquals(TEST_TOKEN, response.getToken());
        }

        @Test
        @DisplayName("TC004 - Login: non-existent user throws UsernameNotFoundException")
        void login_WhenUserNotFound_ThrowsException() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> authService.login(aLoginRequest()));
        }

        @Test
        @DisplayName("TC005 - Login: wrong password throws BadCredentialsException")
        void login_WhenWrongPassword_ThrowsException() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(aUser()));
            when(passwordService.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(false);

            assertThrows(BadCredentialsException.class,
                    () -> authService.login(aLoginRequest()));
        }

        @Test
        @DisplayName("TC025 - Login: username null throws UsernameNotFoundException")
        void login_WhenUsernameNull_ThrowsException() {
            LoginRequest req = new LoginRequest();
            req.setUsername(null);
            req.setPassword(TEST_PASSWORD);
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> authService.login(req));
        }

        @Test
        @DisplayName("TC026 - Login: password null throws BadCredentialsException")
        void login_WhenPasswordNull_ThrowsException() {
            LoginRequest req = new LoginRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword(null);
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(aUser()));
            when(passwordService.matches(null, TEST_ENCODED_PASSWORD)).thenReturn(false);

            assertThrows(BadCredentialsException.class, () -> authService.login(req));
        }

        @Test
        @DisplayName("TC027 - Login: JWT token contains correct subject")
        void login_HappyCase_TokenContainsCorrectSubject() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(aUser()));
            when(passwordService.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(TEST_USERNAME)).thenReturn("validToken_for_newuser");

            AuthResponse response = authService.login(aLoginRequest());

            assertEquals("validToken_for_newuser", response.getToken());
            verify(jwtUtil, times(1)).generateToken(TEST_USERNAME);
        }

        @Test
        @DisplayName("TC101 - Strategy: PasswordStrategy.supports('password') = true")
        void strategy_SupportsPasswordType() {
            assertTrue(passwordStrategy.supports("password"));
            assertTrue(passwordStrategy.supports("PASSWORD"));
            assertTrue(passwordStrategy.supports(null), "null = legacy = password default");
            assertFalse(passwordStrategy.supports("google-oauth"));
        }
    }

    // ============================================================
    // 3. ROLE MANAGEMENT TESTS
    // ============================================================
    @Nested
    @DisplayName("Role management (admin)")
    class RoleManagementTests {

        @Mock UserRepository userRepository;

        @InjectMocks RoleManagementServiceImpl roleManagementService;

        @BeforeEach
        void setUpSecurityContext() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn(ADMIN_USERNAME);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @DisplayName("TC006 - AssignRole happy case updates role")
        void assignRole_HappyCase_UpdatesRole() {
            User user = aUser();
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            roleManagementService.assignRole(TEST_USERNAME, Role.ROLE_ADMIN);

            assertEquals(Role.ROLE_ADMIN, user.getRole());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("TC007 - AssignRole: non-existent user throws exception")
        void assignRole_WhenUserNotFound_ThrowsException() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> roleManagementService.assignRole(TEST_USERNAME, Role.ROLE_ADMIN));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("TC028 - AssignRole: role null throws IllegalArgumentException")
        void assignRole_WhenRoleNull_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> roleManagementService.assignRole(TEST_USERNAME, null));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("TC029 - AssignRole: admin cannot change own role")
        void assignRole_WhenAdminChangesOwnRole_ThrowsException() {
            assertThrows(AccessDeniedException.class,
                    () -> roleManagementService.assignRole(ADMIN_USERNAME, Role.ROLE_USER));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("TC030 - AssignRole: assign ROLE_MODERATOR succeeds")
        void assignRole_AssignModerator_UpdatesRole() {
            User user = aUser();
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            roleManagementService.assignRole(TEST_USERNAME, Role.ROLE_MODERATOR);

            assertEquals(Role.ROLE_MODERATOR, user.getRole());
        }
    }

    // ============================================================
    // 4. PASSWORD SERVICE TESTS (DIP - test abstraction layer)
    // ============================================================
    @Nested
    @DisplayName("PasswordService (DIP abstraction)")
    class PasswordServiceTests {

        @Mock PasswordEncoder passwordEncoder;
        @InjectMocks PasswordServiceImpl passwordService;

        @Test
        @DisplayName("TC102 - encodePassword delegates to encoder")
        void encodePassword_DelegatesToEncoder() {
            when(passwordEncoder.encode("raw")).thenReturn("encoded");

            String result = passwordService.encodePassword("raw");

            assertEquals("encoded", result);
            verify(passwordEncoder, times(1)).encode("raw");
        }

        @Test
        @DisplayName("TC103 - matches delegates to encoder")
        void matches_DelegatesToEncoder() {
            when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);

            assertTrue(passwordService.matches("raw", "encoded"));
            verify(passwordEncoder).matches("raw", "encoded");
        }
    }
}
