package io.ontomesh.events.processor.service;

import io.ontomesh.events.processor.ChannelType;
import io.ontomesh.events.processor.handler.BlankNodeReplacingStatementHandler;
import io.ontomesh.events.processor.provider.IdProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.ontomesh.events.processor.config.Constants.*;

@Component
@Slf4j
public class NotificationService {

    private final IdProvider idProvider;
    private final SubscriptionService subscriptionService;


    public NotificationService(IdProvider idProvider, SubscriptionService subscriptionService) {
        this.idProvider = idProvider;
        this.subscriptionService = subscriptionService;
    }

    /**
     * @param event
     * @return
     */
    public List<Message<String>> createNotificationMessages(Message<String> event) {
        List<Notification> notificationModels = createNotifications(event);
        if (!notificationModels.isEmpty()) {
            return notificationModels.stream()
                    .map(this::createNotificatonMessage)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * @param event
     * @return
     */
    public List<Notification> createNotifications(Message<String> event) {
        List<Notification> notifications = new ArrayList<>();
        Map<IRI, Model> subscribersMap = new HashMap<>();
        IRI eventCode = null, loType = null, loId = null;
        String contentType = new String((byte[]) event.getHeaders().get(MessageHeaders.CONTENT_TYPE), StandardCharsets.UTF_8);
        log.info("Content Type {}", contentType);
        contentType = "application/ld+json";

        if (StringUtils.hasText(contentType)) {
            String eventPayload = event.getPayload();
            // convert to Model
            Model eventModel = convert(eventPayload);
            // read lo id, lo type and event type.
            Set<Resource> subjects = eventModel.subjects();
            for (Resource subject : subjects) {
                IRI iri = Models.subjectIRI(eventModel.filter(subject, RDF.TYPE, LOGISTICS_EVENT)).orElse(null);
                if (iri != null) {
                    loId = Models.objectIRI(eventModel.filter(subject, EVENT_FOR, null)).orElse(null);
                    eventCode = Models.objectIRI(eventModel.filter(subject, EVENT_CODE, null)).orElse(null);
                    loType = Models.objectIRI(eventModel.filter(loId, RDF.TYPE, null)).orElse(null);
                    // get subscriptions
                    subscribersMap = subscriptionService.listSubscribers(loId, loType, eventCode);
                    break;
                }
            }
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

                Model notification = createNotification(loId, eventCode, triggeredBy, loType, false);
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
    public Model createNotification(IRI loId, IRI eventType, IRI triggeredBy, IRI loType,
                                    boolean sendLogisticsObjectBody) {

        ModelBuilder builder = new ModelBuilder()
                .setNamespace("api", "https://onerecord.iata.org/ns/api#")
                .setNamespace("cargo", "https://onerecord.iata.org/ns/cargo#")
                .subject(idProvider.createInternalIri())
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
    private Message<String> createNotificatonMessage(Notification notification) {
        String modelAsString = convert(notification.notification());
        return MessageBuilder.withPayload(modelAsString)
                .setHeader("spring.cloud.stream.sendto.destination", notification.topic())
                .setHeader(MessageHeaders.CONTENT_TYPE, "plain/text")
//                .setHeader(MessageHeaders.ID, idProvider.createInternalIri())
//                .setHeader(MessageHeaders.TIMESTAMP, Instant.now())
                .setHeader("channelType", ChannelType.EMAIL)
                .setHeader("from", null)
                .setHeader("to", "http://1r.ontomesh.io/notifications")
                .build();
    }


    // ***************************************************************************************************************//

    /**
     * Convert Model to String
     *
     * @param model
     * @return
     */
    private String convert(Model model) {
        StringWriter out = new StringWriter();
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
        writer.startRDF();
        for (Statement st : model) {
            writer.handleStatement(st);
        }
        writer.endRDF();
        return out.getBuffer().toString();
    }

    /**
     * Convert String to Model
     *
     * @param eventString
     * @return
     */
    private Model convert(String eventString) {

        BlankNodeReplacingStatementHandler statementHandler = new BlankNodeReplacingStatementHandler(idProvider);
        RDFParser rdfParser = getParser(statementHandler);
        try {
            rdfParser.parse(IOUtils.toInputStream(eventString, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("error parsing the string", e);
            throw new RuntimeException(e.getMessage());
        }
        return statementHandler.getModel();
    }

    /**
     * Parser
     *
     * @return
     */
    private RDFParser getParser(BlankNodeReplacingStatementHandler statementHandler1) {

//        RDFFormat format = null;
//        if (contentType.equals("application/ld+json")) {
//            format = RDFFormat.JSONLD;
//        } else if (contentType.equals("application/turtle")) {
//            format = RDFFormat.TURTLE;
//        }
        RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
        parser.setRDFHandler(statementHandler1);
        return parser;
    }

    /**
     * Notification Holder
     *
     * @param topic
     * @param notification
     */
    record Notification(String topic, Model notification) {

    }
}
