package io.ontomesh.onerecord.server.event;

import io.ontomesh.onerecord.server.services.Producer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class EventProducer implements Producer {

    private final KafkaTemplate<String, String> logisticsEventKafkaTemplate;
    private final String logisticsEventTopic;

    public EventProducer(
            KafkaTemplate<String, String> logisticsEventKafkaTemplate,
            @Value("${logistics.event.topic:logistics-events}") String logisticsEventTopic) {
        this.logisticsEventKafkaTemplate = logisticsEventKafkaTemplate;
        this.logisticsEventTopic = logisticsEventTopic;
    }

    /**
     * @param message
     * @param tenantId
     * @param action
     */
    public void send(String message, IRI tenantId,
                     BiConsumer<? super SendResult<String, String>, ? super Throwable> action) {
        ProducerRecord<String, String> producerRecord;
        producerRecord = new ProducerRecord<>(logisticsEventTopic, message);

        producerRecord
                .headers()
                .add("1r_contentType", "application/turtle".getBytes(StandardCharsets.UTF_8));

        CompletableFuture<SendResult<String, String>> future =
                this.logisticsEventKafkaTemplate.send(producerRecord);

        future.whenComplete(action);
    }
}

