package com.r2s.user.service.management;

import com.r2s.core.event.UserDeletedEvent;
import com.r2s.user.entity.User;
import com.r2s.user.kafka.UserDeletedEventProducer;
import com.r2s.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation cua {@link UserManagementService} - admin operations.
 *
 * <p>Tach rieng theo SRP: chi lo cac thao tac admin (delete...).
 * Profile operations (read/update) o {@link com.r2s.user.service.profile.UserProfileServiceImpl}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserDeletedEventProducer userDeletedEventProducer;

    @Override
    @Transactional
    public void deleteUser(String username) {
        log.info("Deleting user: {}", username);
        // Verify ton tai
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        userRepository.deleteByUsername(username);

        // Publish event (Observer pattern qua Kafka)
        userDeletedEventProducer.sendUserDeletedEvent(new UserDeletedEvent(username));
        log.info("User deleted successfully: {}", username);
    }
}
