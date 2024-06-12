package io.ontomesh.mail.listener;


import io.ontomesh.mail.event.MessageEvent;
import io.ontomesh.mail.processor.MessageEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class MessageEventListener {

    @Autowired
    BindingsLifecycleController bindingsController;

    @Autowired
    private MessageEventProcessor eventProcessor;

    @Bean
    public Consumer<Message<MessageEvent<?>>> listen() {
        return (m) -> {
            MessageHeaders headers = m.getHeaders();
            MessageEvent<?> payload = m.getPayload();
            log.debug("Received message : {}", headers);
            String topic = m.getHeaders().get(KafkaHeaders.TOPIC, String.class);
            Integer partitionId = m.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION, Integer.class);
            log.debug("topic: {}, partition id: {}", topic, partitionId);
//            List<Binding<?>> bindings = bindingsController.queryState("")

            log.debug("message event received of type: {}, with payload: {}", payload.getType(), payload.getPayload());
            eventProcessor.process(headers, payload);
            Acknowledgment acknowledgment = m.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
            if (acknowledgment != null) {
                System.out.println("Acknowledgment provided");
                acknowledgment.acknowledge();
            }
        };
    }
}