package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.User;              // ← Dùng entity của user-service
import com.r2s.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserManagementService {

    private final UserRepository userRepository;

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    }

    @Override
    public UserResponse updateUser(String username, UpdateUserRequest request) {
        User user = findUserOrThrow(username);
        applyUpdates(user, request);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    public void deleteUser(String username) {
        findUserOrThrow(username);
        userRepository.deleteByUsername(username);
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