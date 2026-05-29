package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.User;
import com.r2s.user.kafka.UserDeletedEventProducer;
import com.r2s.user.repository.UserRepository;
import com.r2s.user.service.management.UserManagementServiceImpl;
import com.r2s.user.service.profile.UserProfileServiceImpl;
import com.r2s.user.service.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.r2s.user.testdata.TestDataBuilder.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test cho UserProfileServiceImpl + UserManagementServiceImpl
 * (sau khi tach theo package chuc nang).
 *
 * <p>Test duoc group theo @Nested classes tuong ung voi tung interface:
 * - ProfileOperations: test UserProfileServiceImpl
 * - ManagementOperations: test UserManagementServiceImpl
 * - DtoSecurity: defense-in-depth (DTO khong expose role/password)
 *
 * <p>Su dung TestDataBuilder (Builder Pattern) de tao test data.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User services - SOLID refactored")
class UserServiceTest {

    // ============================================================
    // 1. PROFILE OPERATIONS (UserProfileServiceImpl)
    // ============================================================
    @Nested
    @DisplayName("Profile operations (UserProfileServiceImpl)")
    class ProfileOperations {

        @Mock UserRepository userRepository;
        @Mock UserValidationService userValidationService;   // ← DIP

        @InjectMocks UserProfileServiceImpl userProfileService;

        private User user1;
        private User user2;

        @BeforeEach
        void setUp() {
            user1 = aUser();
            user2 = anAdminUser();
        }

        @Test
        @DisplayName("TC009 - getAllUsers happy case returns list")
        void getAllUsers_HappyCase_ReturnsList() {
            when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

            List<UserResponse> result = userProfileService.getAllUsers();

            assertEquals(2, result.size());
            assertEquals("john", result.get(0).getUsername());
            assertEquals("jane", result.get(1).getUsername());
        }

        @Test
        @DisplayName("TC010 - getAllUsers empty list when no users")
        void getAllUsers_WhenNoUsers_ReturnsEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponse> result = userProfileService.getAllUsers();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC011 - getUserByUsername happy case returns user")
        void getUserByUsername_HappyCase_ReturnsUser() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user1));

            UserResponse result = userProfileService.getUserByUsername(TEST_USERNAME);

            assertNotNull(result);
            assertEquals(TEST_USERNAME, result.getUsername());
            assertEquals(TEST_FULLNAME, result.getFullName());
        }

        @Test
        @DisplayName("TC012 - getUserByUsername user not found throws")
        void getUserByUsername_WhenNotFound_ThrowsException() {
            when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> userProfileService.getUserByUsername("notexist"));
        }

        @Test
        @DisplayName("TC013 - updateUser happy case updates fullName + email")
        void updateUser_HappyCase_UpdatesUser() {
            UpdateUserRequest req = anUpdateRequest();
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user1));
            when(userRepository.save(any(User.class))).thenReturn(user1);

            UserResponse result = userProfileService.updateUser(TEST_USERNAME, req);

            assertNotNull(result);
            assertEquals("John Updated", result.getFullName());
            assertEquals("john_new@test.com", result.getEmail());
            verify(userValidationService).validateUserUpdate(TEST_USERNAME, req);   // Verify DIP
            verify(userRepository, times(1)).save(user1);
        }

        @Test
        @DisplayName("TC014 - updateUser only fullName, email null preserves email")
        void updateUser_WhenOnlyFullName_PreservesEmail() {
            UpdateUserRequest req = anUpdateRequest("Only Name Updated", null);
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user1));
            when(userRepository.save(any(User.class))).thenReturn(user1);

            UserResponse result = userProfileService.updateUser(TEST_USERNAME, req);

            assertEquals("Only Name Updated", result.getFullName());
            assertEquals(TEST_EMAIL, result.getEmail());
        }

        @Test
        @DisplayName("TC015 - updateUser user not found throws")
        void updateUser_WhenUserNotFound_ThrowsException() {
            when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> userProfileService.updateUser("notexist", anUpdateRequest()));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("TC032 - getUserByUsername null throws")
        void getUserByUsername_WhenNull_ThrowsException() {
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());
            assertThrows(UsernameNotFoundException.class,
                    () -> userProfileService.getUserByUsername(null));
        }

        @Test
        @DisplayName("TC033 - getUserByUsername empty throws")
        void getUserByUsername_WhenEmpty_ThrowsException() {
            when(userRepository.findByUsername("")).thenReturn(Optional.empty());
            assertThrows(UsernameNotFoundException.class,
                    () -> userProfileService.getUserByUsername(""));
        }

        @Test
        @DisplayName("TC034 - updateUser invalid email format processes at service")
        void updateUser_WhenInvalidEmailFormat_ProcessesAtServiceLayer() {
            UpdateUserRequest req = anUpdateRequest("John", "abc");
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user1));
            when(userRepository.save(any(User.class))).thenReturn(user1);

            UserResponse result = userProfileService.updateUser(TEST_USERNAME, req);

            assertNotNull(result);
        }

        @Test
        @DisplayName("TC035 - updateUser DB integrity violation propagates")
        void updateUser_WhenEmailExists_ThrowsException() {
            UpdateUserRequest req = anUpdateRequest("John", "existing@test.com");
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user1));
            when(userRepository.save(any(User.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                            "Email already exists"));

            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> userProfileService.updateUser(TEST_USERNAME, req));
        }

        @Test
        @DisplayName("TC038 - updateUser request null throws NPE")
        void updateUser_WhenRequestNull_ThrowsException() {
            doThrow(new NullPointerException("request is null"))
                    .when(userValidationService).validateUserUpdate(TEST_USERNAME, null);

            assertThrows(NullPointerException.class,
                    () -> userProfileService.updateUser(TEST_USERNAME, null));
        }
    }

    // ============================================================
    // 2. MANAGEMENT OPERATIONS (UserManagementServiceImpl)
    // ============================================================
    @Nested
    @DisplayName("Management operations (UserManagementServiceImpl)")
    class ManagementOperations {

        @Mock UserRepository userRepository;
        @Mock UserDeletedEventProducer userDeletedEventProducer;

        @InjectMocks UserManagementServiceImpl userManagementService;

        private User user1;

        @BeforeEach
        void setUp() {
            user1 = aUser();
        }

        @Test
        @DisplayName("TC016 - deleteUser happy case deletes + publishes event")
        void deleteUser_HappyCase_DeletesUser() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user1));
            doNothing().when(userRepository).deleteByUsername(TEST_USERNAME);

            userManagementService.deleteUser(TEST_USERNAME);

            verify(userRepository, times(1)).deleteByUsername(TEST_USERNAME);
            verify(userDeletedEventProducer, times(1))
                    .sendUserDeletedEvent(any(com.r2s.core.event.UserDeletedEvent.class));
        }

        @Test
        @DisplayName("TC017 - deleteUser user not found throws")
        void deleteUser_WhenUserNotFound_ThrowsException() {
            when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> userManagementService.deleteUser("notexist"));
            verify(userRepository, never()).deleteByUsername(anyString());
        }

        @Test
        @DisplayName("TC040 - deleteUser kafka failure propagates")
        void deleteUser_WhenPublishFails_ThrowsException() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user1));
            doNothing().when(userRepository).deleteByUsername(TEST_USERNAME);
            doThrow(new RuntimeException("Kafka broker unavailable"))
                    .when(userDeletedEventProducer)
                    .sendUserDeletedEvent(any(com.r2s.core.event.UserDeletedEvent.class));

            assertThrows(RuntimeException.class,
                    () -> userManagementService.deleteUser(TEST_USERNAME));
        }
    }

    // ============================================================
    // 3. DTO SECURITY (defense-in-depth)
    // ============================================================
    @Nested
    @DisplayName("DTO security (defense-in-depth)")
    class DtoSecurity {

        @Test
        @DisplayName("TC036 - UpdateUserRequest does NOT expose role field")
        void updateUser_DtoDoesNotExposeRole() {
            assertFalse(hasField(UpdateUserRequest.class, "role"),
                    "UpdateUserRequest must NOT have role field");
        }

        @Test
        @DisplayName("TC037 - UpdateUserRequest does NOT expose password field")
        void updateUser_DtoDoesNotExposePassword() {
            assertFalse(hasField(UpdateUserRequest.class, "password"),
                    "UpdateUserRequest must NOT have password field");
        }

        private boolean hasField(Class<?> clazz, String fieldName) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(fieldName)) return true;
            }
            return false;
        }
    }
}
