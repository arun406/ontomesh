package io.ontomesh.onerecord.client.messaging;

import io.ontomesh.onerecord.client.service.NotificationMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class NotificationFunctionConfiguration {

    private final NotificationMessageProcessor processor;

    public NotificationFunctionConfiguration(NotificationMessageProcessor processor) {
        this.processor = processor;
    }

    @Bean
    public Consumer<Message<String>> consumeNotification() {
        return processor::process;
    }
}

