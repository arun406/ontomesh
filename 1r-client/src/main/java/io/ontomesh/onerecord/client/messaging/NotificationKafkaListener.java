package io.ontomesh.onerecord.client.messaging;

import io.ontomesh.onerecord.client.service.NotificationMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka listener for processing notification messages.
 */
@Slf4j
@Component
public class NotificationKafkaListener {

    private final NotificationMessageProcessor processor;

    public NotificationKafkaListener(NotificationMessageProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic:notifications}",
            groupId = "${spring.kafka.consumer.group-id:notification-client-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(Message<String> message) {
        log.info("Received notification message at {} \n  {}", Instant.now(), message.getPayload());
        processor.process(message);
    }
} 