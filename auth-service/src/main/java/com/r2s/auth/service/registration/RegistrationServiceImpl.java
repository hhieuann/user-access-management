package com.r2s.auth.service.registration;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;
import com.r2s.auth.kafka.UserEventProducer;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.service.password.PasswordService;
import com.r2s.core.event.UserRegisteredEvent;
import com.r2s.core.exception.DuplicateUsernameException;
import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation cua {@link RegistrationService}.
 *
 * <p>Phu thuoc PasswordService (abstraction) thay vi BCryptPasswordEncoder
 * truc tiep -> DIP. Co the test bang cach mock PasswordService de doc lap voi BCrypt.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;        // ← DIP
    private final JwtUtil jwtUtil;
    private final UserEventProducer userEventProducer;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        validateUsernameNotTaken(request.getUsername());
        User user = createUserFromRequest(request);
        userRepository.save(user);

        log.info("User registered successfully: {}", request.getUsername());
        publishUserRegisteredEvent(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token);
    }

    private void validateUsernameNotTaken(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Registration failed - username already exists: {}", username);
            throw new DuplicateUsernameException(username);   // ← Domain exception
        }
    }

    private User createUserFromRequest(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordService.encodePassword(request.getPassword()));  // ← DIP
        user.setRole(Role.ROLE_USER);  // Default role
        return user;
    }

    private void publishUserRegisteredEvent(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getUsername(),
                user.getPassword(),
                com.r2s.core.entity.Role.valueOf(user.getRole().name())
        );
        userEventProducer.sendUserRegisteredEvent(event);
    }
}
