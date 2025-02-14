package io.ontomesh.events.processor.messaging;

import io.ontomesh.events.processor.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
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
 */
@Slf4j
@Configuration
@EnableAutoConfiguration
public class LogisticsEventConsumerConfiguration {

    private final StreamBridge streamBridge;
    private final NotificationService notificationService;
    private final Converter<String, Model> stringToModelConverter;

    /**
     * Constructor for LogisticsEventConsumerConfiguration.
     *
     * @param streamBridge           the stream bridge
     * @param stringToModelConverter the converter from String to Model
     * @param notificationService    the notification service
     */
    public LogisticsEventConsumerConfiguration(StreamBridge streamBridge,
                                               @Qualifier("stringToJsonLDConverter")
                                               Converter<String, Model> stringToModelConverter,
                                               NotificationService notificationService) {
        this.streamBridge = streamBridge;
        this.notificationService = notificationService;
        this.stringToModelConverter = stringToModelConverter;
    }

    /**
     * Process Events.
     *
     * @return Consumer of Message
     */
    @Bean
    public Consumer<Message<String>> processEvents() {
        return (Message<String> message) -> {
            String payload = message.getPayload();
            String contentType = getContentType(message.getHeaders());
            IRI notificationId = getNotificationId(message);
            log.debug("Content Type [{}], Key [{}] ", contentType, notificationId);

            Model eventModel = this.stringToModelConverter.convert(payload);
            if (eventModel == null) {
                return;
            }

            List<Message<String>> notificationMessages =
                    this.notificationService.createNotificationMessages(notificationId, eventModel);
            if (notificationMessages.isEmpty()) {
                return;
            }
            notificationMessages.forEach(this::send);
        };
    }

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
     * Send the notification message.
     *
     * @param notificationMessage the notification message
     */
    private void send(Message<String> notificationMessage) {
        this.streamBridge.send("output", notificationMessage);
    }
}
