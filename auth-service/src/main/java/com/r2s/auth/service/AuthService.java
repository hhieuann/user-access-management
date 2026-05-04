package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.User;
import com.r2s.auth.entity.Role;
import com.r2s.auth.kafka.UserEventProducer;
import com.r2s.auth.repository.UserRepository;
import com.r2s.core.event.UserRegisteredEvent;
import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserEventProducer userEventProducer;

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
        log.info("Assigning role {} to user: {}", role, username);

        // TC028: Validate role null TRƯỚC khi query DB
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        // TC029: Admin không được tự đổi role của chính mình
        String currentUsername = SecurityContextHolder
                .getContext().getAuthentication().getName();
        if (currentUsername.equals(username)) {
            throw new AccessDeniedException(
                    "Admin cannot change their own role"
            );
        }

        // Sau đó mới tìm user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        user.setRole(role);
        userRepository.save(user);
        log.info("Role assigned successfully");
    }

    private User buildNewUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
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

    private void validateUsernameNotTaken(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Registration failed - username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists: " + username);
        }
    }
}