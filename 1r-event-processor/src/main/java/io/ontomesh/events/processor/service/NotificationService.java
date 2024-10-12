package io.ontomesh.events.processor.service;

import io.ontomesh.core.BaseIdProvider;
import io.ontomesh.core.enums.ProtocolType;
import io.ontomesh.core.properties.ServerProperties;
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
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.ontomesh.events.processor.config.Constants.*;

@Slf4j
@Service
public class NotificationService {

    private final ServerProperties serverProperties;
    private final Converter<String, Model> stringToModelConverter;
    private final Converter<Model, String> modelToStringConverter;

    private final SubscriptionService subscriptionService;


    public NotificationService(ServerProperties serverProperties, SubscriptionService subscriptionService,
                               @Qualifier("stringToJsonLDConverter") Converter<String, Model> stringToModelConverter,
                               @Qualifier("jsonLDToStringConverter") Converter<Model, String> modelToStringConverter) {
        this.serverProperties = serverProperties;
        this.subscriptionService = subscriptionService;
        this.modelToStringConverter = modelToStringConverter;
        this.stringToModelConverter = stringToModelConverter;
    }

    /**
     * @param payload
     * @param contentType
     * @return
     */
    public List<Message<String>> createNotificationMessages(IRI subject, String payload, String contentType) {

        List<Notification> notificationModels = createNotifications(subject, payload, contentType);

        if (!notificationModels.isEmpty()) {
            return notificationModels.stream()
                    .map(n -> createNotificatonMessage(subject, n))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * @param payload
     * @param contentType
     * @return
     */
    public List<Notification> createNotifications(IRI subject, String payload, String contentType) {
        List<Notification> notifications = new ArrayList<>();
        Map<IRI, Model> subscribersMap = new HashMap<>();
        IRI eventCode = null, loType = null, loId = null;

        if (StringUtils.hasText(contentType)) {
            String eventPayload = payload;
            // convert to Model
            Model eventModel = stringToModelConverter.convert(eventPayload);
            // read lo id, lo type and event type.

            loId = Models.objectIRI(eventModel.filter(subject, EVENT_FOR, null)).orElse(null);
            eventCode = Models.objectIRI(eventModel.filter(subject, EVENT_CODE, null)).orElse(null);
            loType = Models.objectIRI(eventModel.filter(loId, RDF.TYPE, null)).orElse(null);
            // get subscriptions
            subscribersMap = subscriptionService.listSubscribers(loId, loType, eventCode);
        }

        if (!subscribersMap.isEmpty()) {
            for (Map.Entry<IRI, Model> entry : subscribersMap.entrySet()) {
                Model subscription = entry.getValue();
                IRI subscriptionId = entry.getKey();
                IRI triggeredBy = Models.objectIRI(subscription.filter(subscriptionId, SUBSCRIPTION_REQUEST, null))
                        .orElse(null);
                Literal literal = Models.objectLiteral(subscription.filter(subscriptionId, TOPIC, null)).orElse(null);
                String topic = null;
                if (literal != null) {
                    topic = literal.stringValue();
                }

                Model notification = createNotification(subject, loId, eventCode, triggeredBy, loType, false);
                notifications.add(new Notification(topic, notification));
            }
        }
        return notifications;
    }


    /**
     * @param loId                    Logistics Object id
     * @param eventType               Event Type
     * @param triggeredBy             Action Request ID
     * @param loType                  Logistics Object Type
     * @param sendLogisticsObjectBody Indicator to include entire logistics object.
     * @return
     */
    public Model createNotification(IRI subject, IRI loId, IRI eventType, IRI triggeredBy, IRI loType,
                                    boolean sendLogisticsObjectBody) {

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
                .subject(loType)
                .add(RDF.TYPE, XSD.ANYURI);

        if (sendLogisticsObjectBody) {
            builder.subject(loId);
            // TODO: fetch and add all LO statements to builder
        }
        return builder.build();
    }


    /**
     * @param notification
     * @return
     */
    private Message<String> createNotificatonMessage(IRI subject, Notification notification) {

        String modelAsString = modelToStringConverter.convert(notification.model());

        return MessageBuilder.withPayload(modelAsString)
//                .setHeader("spring.cloud.stream.sendto.destination", model.topic())
                .setHeader("protocol", ProtocolType.EMAIL.getValue())
                .setHeader(KafkaHeaders.KEY, subject.stringValue())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
                .setHeader("from", null)
                .setHeader("to", "http://1r.ontomesh.io:8085")
                .build();
    }

    /**
     * Notification Holder
     *
     * @param topic
     * @param model
     */
    record Notification(String topic, Model model) {
    }
}
