package io.ontomesh.events.processor.messaging;

import io.ontomesh.events.processor.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Configuration class for the Logistics Event Consumer.
 * This class is now deprecated in favor of LogisticsEventKafkaListener.
 */
@Slf4j
@Configuration
public class LogisticsEventConsumerConfiguration {

    private final NotificationService notificationService;
    private final Converter<String, Model> stringToModelConverter;

    /**
     * Constructor for LogisticsEventConsumerConfiguration.
     *
     * @param stringToModelConverter the converter from String to Model
     * @param notificationService    the notification service
     */
    public LogisticsEventConsumerConfiguration(
                                               @Qualifier("stringToJsonLDConverter")
                                               Converter<String, Model> stringToModelConverter,
                                               NotificationService notificationService) {
        this.notificationService = notificationService;
        this.stringToModelConverter = stringToModelConverter;
    }

    /**
     * Process Events - DEPRECATED: Use LogisticsEventKafkaListener instead.
     *
     * @return Consumer of Message
     */
    /*
    @Bean
    public Consumer<Message<String>> processEvents() {
        return (Message<String> message) -> {
            String payload = message.getPayload();
            String contentType = getContentType(message.getHeaders());
            IRI notificationId = getNotificationId(message);
            String topicName = getTopicName(message.getHeaders());
            
            log.info("=== EVENT PROCESSOR: INCOMING EVENT RECEIVED ===");
            log.info("Topic: {}", topicName);
            log.info("Event ID: {}", notificationId);
            log.info("Content Type: {}", contentType);
            log.info("Payload Length: {} characters", payload.length());
            log.info("Message Headers: {}", message.getHeaders());
            log.debug("Full Payload: {}", payload);

            Model eventModel = this.stringToModelConverter.convert(payload);
            if (eventModel == null) {
                log.warn("Failed to convert payload to RDF model for event: {} from topic: {}", notificationId, topicName);
                return;
            }
            
            log.info("Successfully converted payload to RDF model for event: {} from topic: {}", notificationId, topicName);
            log.info("Event model contains {} statements", eventModel.size());

            List<Message<String>> notificationMessages =
                    this.notificationService.createNotificationMessages(notificationId, eventModel, topicName);
            
            log.info("Created {} notification messages for event: {} from topic: {}", notificationMessages.size(), notificationId, topicName);
            
            if (notificationMessages.isEmpty()) {
                log.info("No notification messages to send for event: {} from topic: {}", notificationId, topicName);
                return;
            }
            
            log.info("Sending {} notification messages for event: {} from topic: {}", notificationMessages.size(), notificationId, topicName);
            notificationMessages.forEach(msg -> send(msg, topicName));
            log.info("=== EVENT PROCESSOR: EVENT PROCESSING COMPLETED for topic: {} ===", topicName);
        };
    }
    */

    /**
     * Get the notification id.
     *
     * @param message the message
     * @return the notification id
     */
    private static IRI getNotificationId(Message<String> message) {
        return Values.iri((String) Objects.requireNonNull(message.getHeaders().get(KafkaHeaders.RECEIVED_KEY)));
    }

    /**
     * Get the content type.
     *
     * @param headers the message headers
     * @return the content type
     */
    private static String getContentType(MessageHeaders headers) {
        return "application/ld+json";
    }

    /**
     * Get the topic name from message headers.
     *
     * @param headers the message headers
     * @return the topic name
     */
    private static String getTopicName(MessageHeaders headers) {
        // Try to get topic name from Kafka headers
        Object topic = headers.get(KafkaHeaders.RECEIVED_TOPIC);
        if (topic != null) {
            return topic.toString();
        }
        
        // Try alternative header names
        Object topicAlt = headers.get("kafka_receivedTopic");
        if (topicAlt != null) {
            return topicAlt.toString();
        }
        
        // Try to extract from other headers
        for (String key : headers.keySet()) {
            if (key.toLowerCase().contains("topic")) {
                Object value = headers.get(key);
                if (value != null) {
                    return value.toString();
                }
            }
        }
        
        // Fallback to a default if not found
        log.warn("Could not determine topic name from headers: {}", headers.keySet());
        return "unknown-topic";
    }

    /**
     * Send the notification message.
     *
     * @param notificationMessage the notification message
     * @param topicName           the topic name
     */
    /*
    private void send(Message<String> notificationMessage, String topicName) {
        log.info("Sending notification message to output stream (from topic: {})", topicName);
        log.debug("Notification message headers: {}", notificationMessage.getHeaders());
        log.debug("Notification message payload length: {} characters", notificationMessage.getPayload().length());
        
        // Note: StreamBridge removed, just log for now
        log.info("Notification message: {}", notificationMessage.getPayload());
        
        log.info("Notification message sent successfully to output stream (from topic: {})", topicName);
    }
    */
}
