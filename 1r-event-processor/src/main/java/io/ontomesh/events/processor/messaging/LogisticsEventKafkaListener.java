package io.ontomesh.events.processor.messaging;

import io.ontomesh.events.processor.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Spring Kafka listener for processing logistics events.
 */
@Slf4j
@Component
public class LogisticsEventKafkaListener {

    private final NotificationService notificationService;
    private final Converter<String, Model> stringToModelConverter;

    @Value("${kafka.topic.processor:logistics-events}")
    private String topicName;

    /**
     * Constructor for LogisticsEventKafkaListener.
     *
     * @param notificationService    the notification service
     * @param stringToModelConverter the converter from String to Model
     */
    public LogisticsEventKafkaListener(NotificationService notificationService,
                                       @Qualifier("stringToJsonLDConverter")
                                       Converter<String, Model> stringToModelConverter) {
        this.notificationService = notificationService;
        this.stringToModelConverter = stringToModelConverter;
    }

    /**
     * Process logistics events from Kafka topic.
     *
     * @param payload the message payload
     * @param key     the message key
     * @param topic   the topic name
     */
    @KafkaListener(
            topics = "${kafka.topic.processor:logistics-events}",
            groupId = "${kafka.consumer.group-id:logistics-processor-group-03}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processLogisticsEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        
        log.info("=== EVENT PROCESSOR: INCOMING EVENT RECEIVED ===");
        log.info("Topic: {}", topic);
        log.info("Key: {}", key);
        log.info("Payload Length: {} characters", payload.length());
        log.debug("Full Payload: {}", payload);

        try {
            IRI notificationId = Values.iri(key);
            
            log.info("Event ID: {}", notificationId);
            log.info("Content Type: application/ld+json");

            Model eventModel = this.stringToModelConverter.convert(payload);
            if (eventModel == null) {
                log.warn("Failed to convert payload to RDF model for event: {} from topic: {}", notificationId, topic);
                return;
            }
            
            log.info("Successfully converted payload to RDF model for event: {} from topic: {}", notificationId, topic);
            log.info("Event model contains {} statements", eventModel.size());

            List<org.springframework.messaging.Message<String>> notificationMessages =
                    this.notificationService.createNotificationMessages(notificationId, eventModel, topic);
            
            log.info("Created {} notification messages for event: {} from topic: {}", notificationMessages.size(), notificationId, topic);
            
            if (notificationMessages.isEmpty()) {
                log.info("No notification messages to send for event: {} from topic: {}", notificationId, topic);
                return;
            }
            
            log.info("Sending {} notification messages for event: {} from topic: {}", notificationMessages.size(), notificationId, topic);
            // Note: For now, we'll just log the notifications since we removed StreamBridge
            notificationMessages.forEach(msg -> {
                log.info("Notification message: {}", msg.getPayload());
            });
            
            log.info("=== EVENT PROCESSOR: EVENT PROCESSING COMPLETED for topic: {} ===", topic);
            
        } catch (Exception e) {
            log.error("Error processing logistics event from topic: {} with key: {}", topic, key, e);
        }
    }
} 