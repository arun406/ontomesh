package io.ontomesh.onerecord.server.event;

import io.ontomesh.onerecord.server.event.api.Producer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class EventProducer implements Producer {

    private final KafkaTemplate<String, String> logisticsEventKafkaTemplate;
    @Value("${onerecord.logistics.events.destination:logistics-events}")
    private String logisticsEventTopic;

    public EventProducer(
            KafkaTemplate<String, String> logisticsEventKafkaTemplate
    ) {
        this.logisticsEventKafkaTemplate = logisticsEventKafkaTemplate;
    }

    /**
     * Sends a message to the logistics event topic.
     *
     * @param key      the key of the message
     * @param message  the message content
     * @param tenantId the tenant identifier
     * @param action   the callback action to be executed upon completion
     */
    public void send(String key, String message, IRI tenantId,
                     BiConsumer<? super SendResult<String, String>, ? super Throwable> action) {
        log.info("[EventProducer] send() called");
        log.info("[EventProducer] Parameters: topic={}, key={}, tenantId={}, messageLength={}", logisticsEventTopic, key, tenantId, message.length());
        log.debug("[EventProducer] Message content: {}", message);

        // Create ProducerRecord
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(logisticsEventTopic, key, message);
        log.info("[EventProducer] ProducerRecord created for topic: {} with key: {}", logisticsEventTopic, key);
        log.debug("[EventProducer] ProducerRecord initial headers: {}", producerRecord.headers());

        // Add headers
        producerRecord.headers().add(MessageHeaders.CONTENT_TYPE, "text/plain".getBytes(StandardCharsets.UTF_8));
        log.info("[EventProducer] Added CONTENT_TYPE header to ProducerRecord");
        log.debug("[EventProducer] ProducerRecord headers after addition: {}", producerRecord.headers());

        // Before sending
        log.info("[EventProducer] Sending ProducerRecord to Kafka topic: {}", logisticsEventTopic);
        CompletableFuture<SendResult<String, String>> future = this.logisticsEventKafkaTemplate.send(producerRecord);
        log.info("[EventProducer] Send operation submitted to KafkaTemplate for topic: {}", logisticsEventTopic);

        // After sending, attach callback
        future.whenComplete((result, throwable) -> {
            log.info("[EventProducer] Kafka send callback triggered for topic: {}", logisticsEventTopic);
            if (throwable != null) {
                log.error("[EventProducer] Failed to send event to Kafka topic: {} with key: {}", logisticsEventTopic, key, throwable);
                log.error("[EventProducer] Exception: {}", throwable.getMessage(), throwable);
            } else {
                log.info("[EventProducer] Successfully sent event to Kafka topic: {}", logisticsEventTopic);
                log.info("[EventProducer] Key: {}", key);
                log.info("[EventProducer] Partition: {}", result.getRecordMetadata().partition());
                log.info("[EventProducer] Offset: {}", result.getRecordMetadata().offset());
                log.info("[EventProducer] Timestamp: {}", result.getRecordMetadata().timestamp());
                log.info("[EventProducer] Serialized Key Size: {}", result.getRecordMetadata().serializedKeySize());
                log.info("[EventProducer] Serialized Value Size: {}", result.getRecordMetadata().serializedValueSize());
                log.debug("[EventProducer] RecordMetadata: {}", result.getRecordMetadata());
            }
            if (result != null && result.getProducerRecord() != null) {
                log.debug("[EventProducer] ProducerRecord in callback: topic={}, key={}, headers={}",
                        result.getProducerRecord().topic(),
                        result.getProducerRecord().key(),
                        result.getProducerRecord().headers());
            }
            action.accept(result, throwable);
        });
    }


}

