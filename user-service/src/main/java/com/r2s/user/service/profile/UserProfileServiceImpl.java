package com.r2s.user.service.profile;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.User;
import com.r2s.user.repository.UserRepository;
import com.r2s.user.service.validation.UserValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation cua {@link UserProfileService} - quan ly profile cua user.
 *
 * <p>Tach rieng theo SRP: class nay chi lo cac thao tac doc/sua profile.
 * Cac thao tac admin (delete) o {@link com.r2s.user.service.management.UserManagementServiceImpl}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserValidationService userValidationService;   // ← DIP

    @Override
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        log.info("Fetching user: {}", username);
        return userRepository.findByUsername(username)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }

    @Override
    public UserResponse updateUser(String username, UpdateUserRequest request) {
        log.info("Updating user: {}", username);
        userValidationService.validateUserUpdate(username, request);  // ← Tach validation
        User user = findUserOrThrow(username);
        applyUpdates(user, request);
        User saved = userRepository.save(user);
        log.info("User updated successfully: {}", username);
        return UserResponse.fromEntity(saved);
    }

    // ===== Private helpers =====

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    }

    private void applyUpdates(User user, UpdateUserRequest request) {
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
    }
}
