package com.r2s.auth.kafka;

import com.r2s.core.entity.Role;
import com.r2s.core.event.UserRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventProducerTest {

    @Mock
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @InjectMocks
    private UserEventProducer userEventProducer;

    @Test
    @DisplayName("TC008 - SendEvent: Happy case - publish event to correct topic")
    void sendUserRegisteredEvent_HappyCase_PublishesEvent() {
        // Given
        UserRegisteredEvent event = new UserRegisteredEvent("newuser", "encodedPass", Role.ROLE_USER);

        // When
        userEventProducer.sendUserRegisteredEvent(event);

        // Then
        verify(kafkaTemplate, times(1))
                .send(eq("user-registered-topic"), eq("newuser"), eq(event));
    }

    @Test
    @DisplayName("TC031 - SendEvent: Worst case - Kafka send failure throws exception")
    void sendUserRegisteredEvent_WhenKafkaFails_ThrowsException() {
        // Given
        UserRegisteredEvent event = new UserRegisteredEvent("newuser", "encodedPass", com.r2s.core.entity.Role.ROLE_USER);
        when(kafkaTemplate.send(anyString(), anyString(), any(UserRegisteredEvent.class)))
                .thenThrow(new RuntimeException("Kafka broker unavailable"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> userEventProducer.sendUserRegisteredEvent(event));
    }
}