package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.User;              // ← Dùng entity của user-service
import com.r2s.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserManagementService {

    private final UserRepository userRepository;

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
        User user = findUserOrThrow(username);
        applyUpdates(user, request);
        User saved = userRepository.save(user);
        log.info("User updated successfully: {}", username);
        return UserResponse.fromEntity(saved);
    }

    @Override
    public void deleteUser(String username) {
        log.info("Deleting user: {}", username);
        findUserOrThrow(username);
        userRepository.deleteByUsername(username);
        log.info("User deleted successfully: {}", username);
    }

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