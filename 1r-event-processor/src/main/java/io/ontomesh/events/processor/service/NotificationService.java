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

import static io.ontomesh.events.processor.config.Constants.EVENT_CODE;
import static io.ontomesh.events.processor.config.Constants.EVENT_FOR;
import static io.ontomesh.events.processor.config.Constants.SUBSCRIPTION_REQUEST;

/**
 * Service class for managing notifications.
 */
@Slf4j
@Service
public class NotificationService {

    private final ServerProperties serverProperties;
    private final Converter<Model, String> modelToStringConverter;
    private final SubscriptionService subscriptionService;

    /**
     * Constructor for NotificationService.
     *
     * @param serverProperties       the server properties
     * @param subscriptionService    the subscription service
     * @param modelToStringConverter the converter from Model to String
     */
    public NotificationService(ServerProperties serverProperties, SubscriptionService subscriptionService,
                               @Qualifier("jsonLDToStringConverter") Converter<Model, String> modelToStringConverter) {
        this.serverProperties = serverProperties;
        this.subscriptionService = subscriptionService;
        this.modelToStringConverter = modelToStringConverter;

    }

    /**
     * Creates notification messages.
     *
     * @param subject    the subject IRI
     * @param eventModel
     * @return list of notification messages
     */
    public List<Message<String>> createNotificationMessages(IRI subject, Model eventModel) {
        // Extract relevant IRIs and literals directly from the model
        IRI loId = Models.objectIRI(eventModel.filter(subject, EVENT_FOR, null)).orElse(null);
        IRI loType = Models.objectIRI(eventModel.filter(loId, RDF.TYPE, null)).orElse(null);
        Literal eventCodeLit = Models.objectLiteral(eventModel.filter(
                        Models.objectIRI(eventModel.filter(loId, EVENT_CODE, null)).orElse(null),
                        Values.iri("https://onerecord.iata.org/ns/cargo#code"), null))
                .orElse(null);

        // If event code literal is null, return an empty list
        if (eventCodeLit == null) {
            return Collections.emptyList();
        }

        // Derive event type and fetch subscribers
        IRI eventType = Values.iri(eventCodeLit.stringValue());
        Map<IRI, Model> subscribersMap = subscriptionService.listSubscribers(loId, loType, eventType);

        if (subscribersMap.isEmpty()) {
            return Collections.emptyList();
        }

        // Generate notification models
        List<Model> notificationModels = createNotifications(subscribersMap, eventType, loType, loId);

        // Convert models into notification messages or return an empty list if no notifications
        return notificationModels.isEmpty() ? Collections.emptyList() :
                notificationModels.stream()
                        .map(n -> createNotificatonMessage(subject, n))
                        .toList();
    }


    /**
     * Creates notifications.
     *
     * @param subscribersMap the subscribers map
     * @param eventCode      the event code
     * @param loType         the logistics object type
     * @param loId           the logistics object ID
     * @return list of notifications
     */
    public List<Model> createNotifications(Map<IRI, Model> subscribersMap,
                                           IRI eventCode, IRI loType, IRI loId) {
        // Return an empty list if no subscribers exist
        if (subscribersMap.isEmpty()) {
            return Collections.emptyList();
        }

        // Generate notification models for each subscriber
        return subscribersMap.entrySet().stream()
                .map(entry -> {
                    IRI triggeredBy = Models.objectIRI(
                            entry.getValue().filter(entry.getKey(), SUBSCRIPTION_REQUEST, null)
                    ).orElse(null);

                    IRI subscriber = Models.objectIRI(
                            entry.getValue().filter(entry.getKey(), Values.iri("https://onerecord.iata.org/ns/api#hasSubscriber"), null)
                    ).orElse(null);

                    return createNotificationModel(loId, eventCode, triggeredBy, loType, false, subscriber);
                })
                .toList();
    }


    /**
     * Creates a notification model.
     *
     * @param loId                    the logistics object ID
     * @param eventType               the event type
     * @param triggeredBy             the action request ID
     * @param loType                  the logistics object type
     * @param sendLogisticsObjectBody indicator to include entire logistics object
     * @param subscriber
     * @return the notification model
     */
    public Model createNotificationModel(IRI loId, IRI eventType, IRI triggeredBy, IRI loType,
                                         boolean sendLogisticsObjectBody, IRI subscriber) {
        IRI notificationId = new BaseIdProvider(serverProperties.getInternalIriScheme()).createInternalIri();
        //@formatter:off
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
        //@formatter:on
        if (sendLogisticsObjectBody) {
            builder.subject(loId);
        }
        return builder.build();
    }

    /**
     * Creates a notification message.
     *
     * @param subject           the subject IRI
     * @param notificationModel
     * @return the notification message
     */
    private Message<String> createNotificatonMessage(IRI subject, Model notificationModel) {
        IRI subscriber = RDFUtil.getObjectIRI(notificationModel, RDFUtil.getRootIri(notificationModel),
                Values.iri("https://onerecord.iata.org/ns/api#hasSubscriber"), false);

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
