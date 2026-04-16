package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import java.util.List;

public interface UserManagementService {
    // Query
    List<UserResponse> getAllUsers();
    UserResponse getUserByUsername(String username);

    // Command
    UserResponse updateUser(String username, UpdateUserRequest request);
    void deleteUser(String username);
}