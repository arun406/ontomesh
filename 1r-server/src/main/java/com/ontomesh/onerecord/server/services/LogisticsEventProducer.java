package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.enums.EventStatus;
import com.ontomesh.onerecord.server.common.OntoMesh;
import com.ontomesh.onerecord.server.converter.ModelToStringConverter;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LogisticsEventProducer implements EventProducer {

    public final LogisticsObjectRepository repository;
    private static final Logger log = LoggerFactory.getLogger(LogisticsEventProducer.class);

    private final KafkaTemplate<String, String> logisticsEventKafkaTemplate;
    private final String logisticsEventTopic;

    public LogisticsEventProducer(LogisticsObjectRepository repository,
                                  KafkaTemplate<String, String> logisticsEventKafkaTemplate,
                                  @Value("${logistics.event.topic:logistics-events}") String logisticsEventTopic) {
        this.repository = repository;
        this.logisticsEventKafkaTemplate = logisticsEventKafkaTemplate;
        this.logisticsEventTopic = logisticsEventTopic;
    }

    public void send(Model logisticsEventModel, IRI eventId, IRI tenantId) {
        log.debug("Sending event with id [{}]", eventId);
        String eventString = new ModelToStringConverter(RDFFormat.JSONLD).convert(logisticsEventModel);

        CompletableFuture<SendResult<String, String>> future =
                this.logisticsEventKafkaTemplate.send(logisticsEventTopic, tenantId.stringValue(), eventString);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Sent event =[{}] with offset=[{}]", eventId.stringValue(), result.getRecordMetadata().offset());
                this.repository.delete(eventId, OntoMesh.hasEventStatus, OntoMesh.hasEventStatusNew);
                // update the event status.
                this.repository.add(eventId, OntoMesh.hasEventStatus, EventStatus.PUBLISHED.label());
            } else {
                log.debug("Unable to send event=[{}}] due to {}: ", eventId.stringValue(), ex.getMessage());
                this.repository.delete(eventId, OntoMesh.hasEventStatus, OntoMesh.hasEventStatusNew);
                // update the event status.
                this.repository.add(eventId, OntoMesh.hasEventStatus, EventStatus.FAILED.label());
            }
        });
    }
}

