package com.r2s.auth.controller;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.service.authentication.AuthenticationService;
import com.r2s.auth.service.registration.RegistrationService;
import com.r2s.auth.service.role.RoleManagementService;
import com.r2s.core.response.ApiResponse;
import com.r2s.core.response.ResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho cac flow xac thuc.
 *
 * <p>Inject 3 interface RIENG BIET theo ISP/DIP:
 * - AuthenticationService: chi cho login
 * - RegistrationService: chi cho register
 * - RoleManagementService: chi cho admin assign role
 *
 * <p>Moi method dung ResponseBuilder de tra ApiResponse format thong nhat.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final RoleManagementService roleManagementService;
    private final ResponseBuilder responseBuilder;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse result = registrationService.register(request);
        return responseBuilder.buildCreatedResponse(result, "User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse result = authenticationService.login(request);
        return responseBuilder.buildSuccessResponse(result, "Login successful");
    }

    @PostMapping("/admin/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @RequestParam String username,
            @RequestParam Role role) {
        roleManagementService.assignRole(username, role);
        return responseBuilder.buildSuccessResponse(null, "Role assigned successfully");
    }
}
