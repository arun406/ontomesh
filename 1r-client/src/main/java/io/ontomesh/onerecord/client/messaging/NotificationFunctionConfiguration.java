package io.ontomesh.onerecord.client.messaging;

import io.ontomesh.onerecord.client.service.NotificationMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@EnableAutoConfiguration
public class NotificationFunctionConfiguration {

    @Autowired
    private NotificationMessageProcessor processor;

    @Bean
    public Consumer<Message<String>> consumeNotification() {
        return (Message<String> message) -> {
//            processor.process(message);
        };
    }

//    @Bean
//    public Function<String, String> uppercase() {
//        return v -> {
//            System.out.println("processor:::" + processor);
//            return v.toUpperCase();
//        };
//    }

}
