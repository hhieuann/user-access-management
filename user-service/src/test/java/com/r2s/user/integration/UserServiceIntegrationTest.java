package com.r2s.user.integration;

import com.r2s.core.event.UserDeletedEvent;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;
import com.r2s.user.repository.UserRepository;
import com.r2s.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Test: UserService + Repository + DB")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Tạo user test trong DB
        testUser = new User();
        testUser.setUsername("test_user_it");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Original Name");
        testUser.setEmail("original@test.com");
        testUser.setRole(Role.ROLE_USER);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("IT003 - UpdateUser: Cập nhật profile vào DB và query lại verify")
    void updateUser_UpdatesDbAndPersists() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setEmail("updated@test.com");

        // When
        UserResponse response = userService.updateUser("test_user_it", request);

        // Then - verify response
        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("updated@test.com", response.getEmail());

        // Verify DB - data thực sự được persist
        Optional<User> updatedUser = userRepository.findByUsername("test_user_it");
        assertTrue(updatedUser.isPresent());
        assertEquals("Updated Name", updatedUser.get().getFullName());
        assertEquals("updated@test.com", updatedUser.get().getEmail());
    }

    @Test
    @DisplayName("IT004 - DeleteUser: User bị xoá khỏi DB")
    void deleteUser_RemovesFromDb() {
        // Given - verify user đã tồn tại
        assertTrue(userRepository.findByUsername("test_user_it").isPresent());

        // When
        userService.deleteUser("test_user_it");

        // Then - verify user đã bị xoá
        Optional<User> deletedUser = userRepository.findByUsername("test_user_it");
        assertFalse(deletedUser.isPresent(), "User phải bị xoá khỏi DB");
    }
}