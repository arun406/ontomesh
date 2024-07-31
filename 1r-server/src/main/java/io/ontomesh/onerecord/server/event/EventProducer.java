package io.ontomesh.onerecord.server.event;

import io.ontomesh.onerecord.server.services.Producer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
     */
    public void send(String message, IRI tenantId) {
        ProducerRecord<String, String> producerRecord;
        producerRecord = new ProducerRecord<>(logisticsEventTopic, message);

        producerRecord
                .headers()
                .add(MessageHeaders.ID, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                .add(MessageHeaders.CONTENT_TYPE, "text/plain".getBytes(StandardCharsets.UTF_8));

        try {
            logisticsEventKafkaTemplate.send(producerRecord).get(10, TimeUnit.SECONDS);
            handleSuccess(message);
        } catch (ExecutionException e) {
            handleFailure(message, producerRecord, e.getCause());
        } catch (TimeoutException | InterruptedException e) {
            handleFailure(message, producerRecord, e);
        }

    }

    private void handleFailure(String message, ProducerRecord<String, String> producerRecord, Throwable cause) {
        log.error("Error occurred while publishing the message.");
//        getRepository().update(logisticsEvent.iri(), OntoMesh.hasEventStatus,
//                OntoMesh.hasEventStatusNew, EventStatus.FAILED.label());
    }

    private void handleSuccess(String message) {
        log.info("Message published successfully.");
//        getRepository().update(logisticsEvent.iri(), OntoMesh.hasEventStatus,
//                OntoMesh.hasEventStatusNew, EventStatus.PUBLISHED.label());
    }


}

