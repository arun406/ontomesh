package io.ontomesh.events.processor.service;

import io.ontomesh.core.BaseIdProvider;
import io.ontomesh.core.enums.ProtocolType;
import io.ontomesh.core.properties.ServerProperties;
import io.ontomesh.core.rdf.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ontomesh.events.processor.config.Constants.*;

@Slf4j
@Service
public class NotificationService {

    private final ServerProperties serverProperties;
    private final Converter<Model, String> modelToStringConverter;
    private final SubscriptionService subscriptionService;

    /**
     * Constructor for NotificationService.
     *
     * @param serverProperties       the server properties containing internal configuration
     * @param subscriptionService    service responsible for handling subscriptions
     * @param modelToStringConverter converter for transforming RDF models into string representation
     */
    public NotificationService(ServerProperties serverProperties, SubscriptionService subscriptionService,
                               @Qualifier("jsonLDToStringConverter") Converter<Model, String> modelToStringConverter) {
        this.serverProperties = serverProperties;
        this.subscriptionService = subscriptionService;
        this.modelToStringConverter = modelToStringConverter;
    }

    /**
     * Generates a list of notification messages based on an event.
     *
     * @param subject    the subject IRI representing the event entity
     * @param eventModel the RDF model containing event details
     * @return a list of notification messages to be sent, or an empty list if no notifications are needed
     */
    public List<Message<String>> createNotificationMessages(IRI subject, Model eventModel) {
        Optional<IRI> loIdOpt = Models.objectIRI(eventModel.filter(subject, EVENT_FOR, null));
        if (loIdOpt.isEmpty()) return Collections.emptyList();

        IRI loId = loIdOpt.get();
        IRI loType = Models.objectIRI(eventModel.filter(loId, RDF.TYPE, null)).orElse(null);
        IRI eventType = extractEventType(eventModel, loId);

        if (eventType == null) return Collections.emptyList();

        Map<IRI, Model> subscribersMap = subscriptionService.listSubscribers(loId, loType, eventType);
        if (subscribersMap.isEmpty()) return Collections.emptyList();

        return createNotifications(subscribersMap, eventType, loType, loId).stream()
                .map(n -> createNotificationMessage(subject, n))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the event type from the event model.
     *
     * @param eventModel the RDF model containing event details
     * @param loId       the IRI of the logistics object associated with the event
     * @return the event type as an IRI, or null if it cannot be determined
     */
    private IRI extractEventType(Model eventModel, IRI loId) {
        return Models.objectLiteral(eventModel.filter(
                        Models.objectIRI(eventModel.filter(loId, EVENT_CODE, null)).orElse(null),
                        CODE, null))
                .map(Literal::stringValue)
                .map(Values::iri)
                .orElse(null);
    }

    /**
     * Creates a list of notification models for all subscribers of a given event.
     *
     * @param subscribersMap a map of subscriber IRIs to their subscription models
     * @param eventCode      the event type IRI
     * @param loType         the logistics object type IRI
     * @param loId           the logistics object ID IRI
     * @return a list of notification models, or an empty list if no notifications need to be sent
     */
    public List<Model> createNotifications(Map<IRI, Model> subscribersMap, IRI eventCode, IRI loType, IRI loId) {
        if (subscribersMap.isEmpty()) return Collections.emptyList();

        return subscribersMap.entrySet().stream()
                .map(entry -> createNotificationModel(
                        loId,
                        eventCode,
                        Models.objectIRI(entry.getValue().filter(entry.getKey(), SUBSCRIPTION_REQUEST, null)).orElse(null),
                        loType,
                        false,
                        Models.objectIRI(entry.getValue().filter(entry.getKey(), SUBSCRIBER, null)).orElse(null)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Constructs a notification model containing details about the event.
     *
     * @param loId                    the logistics object ID IRI
     * @param eventType               the event type IRI
     * @param triggeredBy             the action request IRI that triggered this event
     * @param loType                  the logistics object type IRI
     * @param sendLogisticsObjectBody flag indicating whether to include full logistics object details
     * @param subscriber              the subscriber IRI receiving the notification
     * @return an RDF model representing the notification
     */
    public Model createNotificationModel(IRI loId, IRI eventType, IRI triggeredBy, IRI loType,
                                         boolean sendLogisticsObjectBody, IRI subscriber) {
        IRI notificationId = new BaseIdProvider(serverProperties.getInternalIriScheme()).createInternalIri();

        ModelBuilder builder = new ModelBuilder()
                .setNamespace("api", "https://onerecord.iata.org/ns/api#")
                .setNamespace("cargo", "https://onerecord.iata.org/ns/cargo#")
                .subject(notificationId)
                .add(RDF.TYPE, Values.iri("api:Notification"))
                .add("api:hasLogisticsObject", loId)
                .add("api:hasEventType", eventType)
                .add("api:isTriggeredBy", triggeredBy)
                .add("api:hasLogisticsObjectType", loType)
                .add("api:hasSubscriber", subscriber)
                .subject(loType)
                .add(RDF.TYPE, XSD.ANYURI);

        if (sendLogisticsObjectBody) {
            builder.subject(loId);
        }

        return builder.build();
    }

    /**
     * Converts a notification model into a Kafka message.
     *
     * @param subject           the subject IRI representing the event entity
     * @param notificationModel the RDF model containing notification details
     * @return a Kafka message containing the notification payload
     */
    private Message<String> createNotificationMessage(IRI subject, Model notificationModel) {
        IRI subscriber = RDFUtil.getObjectIRI(notificationModel, RDFUtil.getRootIri(notificationModel),
                SUBSCRIBER, false);

        String modelAsString = modelToStringConverter.convert(notificationModel);

        return MessageBuilder.withPayload(modelAsString)
                .setHeader("protocol", ProtocolType.HTTP.getValue())
                .setHeader(KafkaHeaders.KEY, subject.stringValue())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
                .setHeader("from", null)
                .setHeader("to", subscriber.stringValue())
                .build();
    }
}
