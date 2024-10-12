package io.ontomesh.events.processor.messaging;

import io.ontomesh.events.processor.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.List;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 */
@Slf4j
@Configuration
@EnableAutoConfiguration
public class LogisticsEventConsumerConfiguration {

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private NotificationService notificationService;

    @Bean
    public Consumer<Message<String>> processEvents() {
        return (Message<String> message) -> {
            String payload = message.getPayload();
            String contentType = getContentType(message.getHeaders());
            Object object = message.getHeaders().get(KafkaHeaders.RECEIVED_KEY);
            String key = (String) object;
            IRI notificationId = Values.iri(key);
            log.info("Content Type [{}], Key [{}] ", contentType, notificationId);

            List<Message<String>> notificationMessages = notificationService
                    .createNotificationMessages(notificationId, payload, contentType);

            for (Message<String> notificationMessage : notificationMessages) {
                streamBridge.send("output", notificationMessage);
            }
        };
    }

    private static @NotNull String getContentType(MessageHeaders headers) {
        Object header = headers.get(MessageHeaders.CONTENT_TYPE);
        String contentType = new String((byte[]) header, UTF_8);
        contentType = "application/ld+json";
        return contentType;
    }

}
