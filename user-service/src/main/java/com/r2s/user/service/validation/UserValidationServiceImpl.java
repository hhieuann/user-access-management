package com.r2s.user.service.validation;

import com.r2s.core.exception.BusinessException;
import com.r2s.core.exception.DuplicateEmailException;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Implementation cua {@link UserValidationService}.
 *
 * <p>Tap trung TAT CA validation logic vao 1 cho - tranh trung lap (DRY).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationServiceImpl implements UserValidationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private final UserRepository userRepository;

    @Override
    public void validateUserUpdate(String username, UpdateUserRequest request) {
        if (request == null) {
            throw new BusinessException("Update request cannot be null");
        }
        if (request.getEmail() != null) {
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                throw new BusinessException("Invalid email format: " + request.getEmail());
            }
            validateEmailUnique(request.getEmail(), username);
        }
        if (request.getFullName() != null && request.getFullName().isBlank()) {
            throw new BusinessException("Full name cannot be blank");
        }
    }

    @Override
    public void validateEmailUnique(String email, String currentUsername) {
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            // Allow user update voi email cua chinh ho
            if (!existingUser.getUsername().equals(currentUsername)) {
                log.warn("Duplicate email detected: {}", email);
                throw new DuplicateEmailException(email);
            }
        });
    }
}
