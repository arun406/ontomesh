package io.ontomesh.onerecord.client.messaging;

import io.ontomesh.onerecord.client.service.NotificationMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class NotificationFunctionConfiguration {

    @Autowired
    private NotificationMessageProcessor processor;

    @Bean
    public Consumer<Message<String>> consumeNotification() {
        return (Message<String> message) -> {
            processor.process(message);
        };
    }
}
