package com.r2s.user.kafka;

import com.r2s.core.event.UserRegisteredEvent;
import com.r2s.user.entity.User;
import com.r2s.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserEventConsumer userEventConsumer;

    private UserRegisteredEvent event;

    @BeforeEach
    void setUp() {
        event = new UserRegisteredEvent(
                "newuser",
                "encodedPassword",
                com.r2s.core.entity.Role.ROLE_USER
        );
    }

    @Test
    @DisplayName("TC018 - HandleEvent: Happy case - save new user")
    void handleUserRegistered_HappyCase_SavesUser() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        // When
        userEventConsumer.handleUserRegistered(event);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("TC019 - HandleEvent: Worst case - user already exists, do nothing")
    void handleUserRegistered_WhenUserExists_DoesNothing() {
        // Given
        User existingUser = new User();
        existingUser.setUsername("newuser");
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));

        // When
        userEventConsumer.handleUserRegistered(event);

        // Then
        verify(userRepository, never()).save(any(User.class));
    }
}