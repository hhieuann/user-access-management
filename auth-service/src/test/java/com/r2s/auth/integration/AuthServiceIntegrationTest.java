package com.r2s.auth.integration;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.service.AuthService;
import com.r2s.core.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Test: AuthService + Repository + DB")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Clean DB trước mỗi test
        userRepository.deleteAll();

        // Mock SecurityContext
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("IT001 - Register: User được lưu vào DB H2 thật và query lại tìm thấy")
    void register_SavesToDbAndCanBeFound() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integration_user");
        request.setPassword("password123");

        // When
        AuthResponse response = authService.register(request);

        // Then - verify response
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify DB - user thực sự tồn tại trong H2
        Optional<User> savedUser = userRepository.findByUsername("integration_user");
        assertTrue(savedUser.isPresent());
        assertEquals("integration_user", savedUser.get().getUsername());
        assertEquals(Role.ROLE_USER, savedUser.get().getRole());
        assertNotEquals("password123", savedUser.get().getPassword(),
                "Password phải được mã hóa, không lưu plain text");
    }

    @Test
    @DisplayName("IT002 - Register: Duplicate username throws exception")
    void register_WhenDuplicateUsername_ThrowsException() {
        // Given - đăng ký user lần 1
        RegisterRequest request = new RegisterRequest();
        request.setUsername("duplicate_user");
        request.setPassword("password123");
        authService.register(request);

        // When - đăng ký user lần 2 với cùng username
        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("duplicate_user");
        duplicate.setPassword("anotherpassword");

        // Then - throw exception
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(duplicate)
        );
        assertTrue(ex.getMessage().contains("already exists"));

        // Verify DB - chỉ có 1 user
        long count = userRepository.findAll().stream()
                .filter(u -> "duplicate_user".equals(u.getUsername()))
                .count();
        assertEquals(1, count, "DB chỉ có 1 user duplicate_user");
    }
}