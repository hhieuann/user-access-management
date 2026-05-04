package com.r2s.user.kafka;

import com.r2s.core.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeletedEventProducer {

    public static final String TOPIC = "user-deleted-topic";
    private final KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

    public void sendUserDeletedEvent(UserDeletedEvent event) {
        log.info("Sending UserDeleted event: {}", event.getUsername());
        kafkaTemplate.send(TOPIC, event.getUsername(), event);
    }
}