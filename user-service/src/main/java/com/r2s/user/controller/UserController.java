package com.r2s.user.controller;

import com.r2s.core.response.ApiResponse;
import com.r2s.core.response.ResponseBuilder;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.service.management.UserManagementService;
import com.r2s.user.service.profile.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho user management.
 *
 * <p>Inject 2 interface rieng theo ISP/DIP:
 * - UserProfileService: cho user xem/sua profile cua minh
 * - UserManagementService: cho admin (delete)
 *
 * <p>Tat ca response wrap qua ResponseBuilder -> format thong nhat.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final UserManagementService userManagementService;
    private final ResponseBuilder responseBuilder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return responseBuilder.buildSuccessResponse(
                userProfileService.getAllUsers(),
                "Users retrieved successfully"
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication authentication) {
        UserResponse profile = userProfileService.getUserByUsername(authentication.getName());
        return responseBuilder.buildSuccessResponse(profile, "Profile retrieved successfully");
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updated = userProfileService.updateUser(authentication.getName(), request);
        return responseBuilder.buildSuccessResponse(updated, "Profile updated successfully");
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userManagementService.deleteUser(username);
        return responseBuilder.buildNoContentResponse();
    }
}
