package com.r2s.auth.service.role;

import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation cua {@link RoleManagementService}.
 *
 * <p>Chua admin-only logic: assign role. Tach rieng khoi
 * login/register de tuan thu SRP (Single Responsibility).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleManagementServiceImpl implements RoleManagementService {

    private final UserRepository userRepository;

    @Override
    public void assignRole(String username, Role role) {
        log.info("Assigning role {} to user: {}", role, username);

        validateRoleNotNull(role);
        ensureAdminNotChangingOwnRole(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        user.setRole(role);
        userRepository.save(user);
        log.info("Role assigned successfully");
    }

    // ===== Private helpers (DRY + clean code) =====

    private void validateRoleNotNull(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
    }

    private void ensureAdminNotChangingOwnRole(String targetUsername) {
        String currentUsername = SecurityContextHolder
                .getContext().getAuthentication().getName();
        if (currentUsername.equals(targetUsername)) {
            throw new AccessDeniedException("Admin cannot change their own role");
        }
    }
}
