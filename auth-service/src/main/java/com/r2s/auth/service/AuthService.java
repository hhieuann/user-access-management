package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.User;              // ← Dùng entity của auth-service
import com.r2s.auth.entity.Role;              // ← Dùng Role của auth-service
import com.r2s.auth.kafka.UserEventProducer;
import com.r2s.auth.repository.UserRepository;
import com.r2s.core.event.UserRegisteredEvent;
import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserEventProducer userEventProducer; // ← Thêm

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        validateUsernameNotTaken(request.getUsername());
        User user = buildNewUser(request);
        userRepository.save(user);
        log.info("User registered successfully: {}", request.getUsername());

        // Publish event sang Kafka
        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getUsername(),
                user.getPassword(),
                user.getFullName(),
                user.getEmail(),
                com.r2s.core.entity.Role.valueOf(user.getRole().name())
        );
        userEventProducer.sendUserRegisteredEvent(event);

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        User user = findUserOrThrow(request.getUsername());
        verifyPassword(request.getPassword(), user.getPassword());
        log.info("User logged in successfully: {}", request.getUsername());
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token);
    }

    @Override
    public void assignRole(String username, Role role) {
        log.info("Assigning role {} to user: {}", role, username); // ← Thêm log
        User user = findUserOrThrow(username);
        user.setRole(role);
        userRepository.save(user);
        log.info("Role assigned successfully to user: {}", username); // ← Thêm log
    }

    private void validateUsernameNotTaken(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Registration failed - username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists: " + username);
        }
    }

    private User buildNewUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());  // ← Thêm
        user.setEmail(request.getEmail());        // ← Thêm
        user.setRole(Role.ROLE_USER);
        return user;
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    }

    private void verifyPassword(String raw, String encoded) {
        if (!passwordEncoder.matches(raw, encoded)) {
            throw new BadCredentialsException("Invalid password");
        }
    }
}