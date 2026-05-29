package com.r2s.auth.integration;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.service.registration.RegistrationService;
import com.r2s.core.event.UserRegisteredEvent;
import com.r2s.core.exception.DuplicateUsernameException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration Test su dung H2 in-memory database (mode PostgreSQL).
 *
 * <p>Sau refactor SOLID Round 3: inject RegistrationService (interface)
 * thay vi AuthService (concrete) - DIP applied.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Test: RegistrationService + Repository + DB")
class AuthServiceIntegrationTest {

    @Autowired
    private RegistrationService registrationService;   // ← DIP: inject interface

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Mock SecurityContext (admin đã login)
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("IT001 - Register: user được lưu vào DB và query lại tìm thấy")
    void register_SavesToDbAndCanBeFound() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integration_user");
        request.setPassword("password123");

        AuthResponse response = registrationService.register(request);

        assertNotNull(response);
        assertNotNull(response.getToken());

        Optional<User> savedUser = userRepository.findByUsername("integration_user");
        assertTrue(savedUser.isPresent());
        assertEquals("integration_user", savedUser.get().getUsername());
        assertEquals(Role.ROLE_USER, savedUser.get().getRole());
        assertNotEquals("password123", savedUser.get().getPassword(),
                "Password phải được mã hóa, không lưu plain text");
    }

    @Test
    @DisplayName("IT002 - Register: duplicate username throws DuplicateUsernameException")
    void register_WhenDuplicateUsername_ThrowsDuplicateException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("duplicate_user");
        request.setPassword("password123");
        registrationService.register(request);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("duplicate_user");
        duplicate.setPassword("anotherpassword");

        DuplicateUsernameException ex = assertThrows(
                DuplicateUsernameException.class,
                () -> registrationService.register(duplicate)
        );
        assertTrue(ex.getMessage().contains("already exists"));

        long count = userRepository.findAll().stream()
                .filter(u -> "duplicate_user".equals(u.getUsername()))
                .count();
        assertEquals(1, count);
    }
}
