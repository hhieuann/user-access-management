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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Test: UserService + Repository + DB")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

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
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setEmail("updated@test.com");

        UserResponse response = userService.updateUser("test_user_it", request);

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("updated@test.com", response.getEmail());

        Optional<User> updatedUser = userRepository.findByUsername("test_user_it");
        assertTrue(updatedUser.isPresent());
        assertEquals("Updated Name", updatedUser.get().getFullName());
    }

    @Test
    @DisplayName("IT004 - DeleteUser: User bị xoá khỏi DB")
    void deleteUser_RemovesFromDb() {
        assertTrue(userRepository.findByUsername("test_user_it").isPresent());

        userService.deleteUser("test_user_it");

        Optional<User> deletedUser = userRepository.findByUsername("test_user_it");
        assertFalse(deletedUser.isPresent());
    }
}