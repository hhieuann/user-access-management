package com.r2s.user.kafka;

import com.r2s.core.event.UserRegisteredEvent;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;
import com.r2s.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserRepository userRepository;

    @KafkaListener(
            topics = "user-registered-topic",
            groupId = "user-service-group"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegistered event: {}", event.getUsername());

        // Kiểm tra user đã tồn tại chưa
        if (userRepository.findByUsername(event.getUsername()).isPresent()) {
            log.warn("User already exists in user-service: {}", event.getUsername());
            return;
        }

        // Tạo user trong user-service DB
        User user = new User();
        user.setUsername(event.getUsername());
        user.setPassword(event.getPassword()); // Đã được hash từ auth-service
        user.setFullName(event.getFullName());
        user.setEmail(event.getEmail());
        user.setRole(Role.valueOf(event.getRole().name()));

        userRepository.save(user);
        log.info("User synced to user-service: {}", event.getUsername());
    }
}