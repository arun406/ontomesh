package io.ontomesh.onerecord.server.event;

import io.ontomesh.onerecord.server.api.enums.RDFConverterType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.config.OneRecordProperties;
import io.ontomesh.onerecord.server.converter.ConverterFactory;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static io.ontomesh.onerecord.server.api.enums.RDFConverterType.STRING_TO_JSONLD_CONVERTER;
import static io.ontomesh.onerecord.server.api.enums.RDFConverterType.STRING_TO_TURTLE_CONVERTER;

@Slf4j
@Component
public class EventConsumer {

    public final OneRecordProperties oneRecordProperties;
    private final ConverterFactory converterFactory;

    private final IdProvider idProvider;

    public EventConsumer(ConverterFactory converterFactory, OneRecordProperties oneRecordProperties, IdProvider idProvider) {
        this.converterFactory = converterFactory;
        this.oneRecordProperties = oneRecordProperties;
        this.idProvider = idProvider;
    }

    @SuppressWarnings("unchecked")
    private Model convertToModel(RDFFormat rdfFormat, String event) {
        RDFConverterType converterType = null;
        if (rdfFormat.equals(RDFFormat.TURTLE)) {
            converterType = STRING_TO_TURTLE_CONVERTER;
        } else if (rdfFormat.equals(RDFFormat.JSONLD)) {
            converterType = STRING_TO_JSONLD_CONVERTER;
        }
        return (Model) this.converterFactory.get(converterType)
                .convert(event);
    }

    /**
     * @param model
     * @return
     */
    private Model convertToNotification(Model model) {
        DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();
        IRI hasEventType = null;
        IRI hasLogisticsObject, hasLogisticsObjectType;
        IRI eventId = Models.subjectIRI(model.filter(null, RDF.TYPE, CARGO.LogisticsEvent)).orElseGet(null);
        if (eventId == null)
            return emptyModel;
        hasLogisticsObject = Models.objectIRI(model.filter(eventId, CARGO.eventFor, null)).orElseGet(null);
        if (hasLogisticsObject == null)
            return emptyModel;

        hasLogisticsObjectType = Models.objectIRI(model.filter(hasLogisticsObject, RDF.TYPE, null)).orElseGet(null);
        IRI eventCodeIRI = Models.objectIRI(model.filter(eventId, CARGO.eventCode, null)).orElseGet(null);

        if (eventCodeIRI != null) {
            hasEventType = Models.objectIRI(model.filter(eventCodeIRI, CARGO.code, null)).orElseGet(null);
            if (hasEventType == null) {
                return emptyModel;
            }
        }
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(idProvider.createUniqueNotificationEvent(hasLogisticsObject).iri())
                .add(RDF.TYPE, API.Notification)
                .add(API.hasEventType, hasEventType)
                .add(API.hasLogisticsObject, hasLogisticsObject)
                .add(API.hasLogisticsObjectType, hasLogisticsObjectType)
                .subject(hasLogisticsObjectType)
                .add(RDF.TYPE, XSD.ANYURI)
//                .add(API.isTriggeredBy, isTriggeredBy)
                .build();
    }

    @KafkaListener(topics = "#{__listener.oneRecordProperties.client.eventsTopic}",
            groupId = "#{__listener.oneRecordProperties.client.eventTopicGroupId}")
    public void processMessage(@Payload String event,
                               @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) Integer key,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long ts,
                               @Header("1r_contentType") String contentType) {

        log.info("Message received:  {}", event);

        log.info("Content Type: {}\n Key : {} \n Partition : {} \n Topic: {} \n Timestamp : {} \n", contentType, key, partition, topic, ts);

        MediaType mediaType = SemanticWebMediaType.valueOf(contentType);
        RDFFormat rdfFormat = RDFUtil.getFormat(mediaType);
        Model model = convertToModel(rdfFormat, event);

        log.info("******************Logistics-Event**************************");
        RDFUtil.print(model, rdfFormat);

        // convert to notification.
        Model notification = convertToNotification(model);

        log.info("******************Notification**************************");
        RDFUtil.print(notification, RDFFormat.TURTLE);
    }
}
