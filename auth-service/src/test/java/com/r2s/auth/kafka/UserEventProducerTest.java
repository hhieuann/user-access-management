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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}