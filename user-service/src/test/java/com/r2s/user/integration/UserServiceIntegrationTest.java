package com.r2s.user.integration;

import com.r2s.core.event.UserDeletedEvent;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;
import com.r2s.user.repository.UserRepository;
import com.r2s.user.service.management.UserManagementService;
import com.r2s.user.service.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test sau khi refactor SOLID:
 * - Inject 2 interface RIENG (UserProfileService + UserManagementService) - ISP
 * - Test full flow voi DB that
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration: UserProfileService + UserManagementService + DB")
class UserServiceIntegrationTest {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserManagementService userManagementService;
    @Autowired private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User testUser = new User();
        testUser.setUsername("test_user_it");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Original Name");
        testUser.setEmail("original@test.com");
        testUser.setRole(Role.ROLE_USER);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("IT003 - updateUser: cập nhật profile vào DB và query lại verify")
    void updateUser_UpdatesDbAndPersists() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setEmail("updated@test.com");

        UserResponse response = userProfileService.updateUser("test_user_it", request);

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("updated@test.com", response.getEmail());

        Optional<User> updatedUser = userRepository.findByUsername("test_user_it");
        assertTrue(updatedUser.isPresent());
        assertEquals("Updated Name", updatedUser.get().getFullName());
    }

    @Test
    @DisplayName("IT004 - deleteUser: user bị xoá khỏi DB")
    void deleteUser_RemovesFromDb() {
        assertTrue(userRepository.findByUsername("test_user_it").isPresent());

        userManagementService.deleteUser("test_user_it");

        Optional<User> deletedUser = userRepository.findByUsername("test_user_it");
        assertFalse(deletedUser.isPresent());
    }
}
