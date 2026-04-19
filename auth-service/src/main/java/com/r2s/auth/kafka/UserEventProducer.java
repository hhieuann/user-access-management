package com.r2s.auth.kafka;

import com.r2s.core.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    public static final String USER_REGISTERED_TOPIC = "user-registered-topic";

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Sending UserRegistered event: {}", event.getUsername());
        kafkaTemplate.send(USER_REGISTERED_TOPIC, event.getUsername(), event);
    }
}