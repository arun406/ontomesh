package io.ontomesh.events.processor.config;

import io.ontomesh.events.processor.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class ApplicationConfig {
    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private NotificationService notificationService;

    @Bean
    public Consumer<Message<String>/*, List<Message<String>>*/> eventProcessor() {
        return (Message<String> message) -> {
            List<Message<String>> notificationMessages = notificationService.createNotificationMessages(message);
            for (Message<String> notificationMessage : notificationMessages) {
                streamBridge.send("output", notificationMessage);
            }
        };
    }


   /* @Bean
    public NewDestinationBindingCallback<KafkaProducerProperties> dynamicBindingConfigurer() {
        return (destinationName, channel, producerProperties, extendedProducerProperties) -> {
            log.debug("in dynamicBindingConfigurer....");
        };
    }*/
}
