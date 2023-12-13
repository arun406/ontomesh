package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.enums.NotificationEventType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import com.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static com.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;

@Slf4j
@Service
public class SubscriptionService extends AbstractLogisticsObjectService {

    private final LogisticsEventService logisticsEventService;

    @Value("${subscriptions.sendLogisticsObjectBody:false}")
    private boolean sendLogisticsObjectBody;

    @Value("${subscriptions.subscribeToLogisticsEvents:true}")
    private boolean subscribeToLogisticsEvents;
    @Value("${subscriptions.notifyRequestStatusChange:true}")
    private boolean notifyRequestStatusChange;

    public SubscriptionService(LogisticsObjectRepository repository, IdProvider idProvider,
                               LogisticsEventService logisticsEventService) {
        super(repository, idProvider);
        this.logisticsEventService = logisticsEventService;
    }

    /**
     * @param topic
     * @param topicType
     * @return
     */
    public LogisticsObject createSubscriptionProposalResponse(IRI topic, IRI topicType) {

//        IRI subscriber = Values.iri(companyIdentifier);
        IRI subscriptionIRI = idProvider.createUniqueLoUri().iri();
        ModelBuilder modelBuilder = new ModelBuilder()
                .subject(subscriptionIRI)
                .add(RDF.TYPE, API.Subscription)
                .add(API.hasSubscriber, idProvider.tenant())
                .add(API.hasTopic, topic)
                .add(API.hasTopicType, topicType)
//                .add(API.subscribeToLogisticsEvents, Values.literal(subscribeToLogisticsEvents))
                .add(API.notifyRequestStatusChange, Values.literal(notifyRequestStatusChange))
                .add(API.expiresAt, Values.literal(Instant.now().toString(), CoreDatatype.XSD.DATETIME))
                .add(API.sendLogisticsObjectBody, Values.literal(sendLogisticsObjectBody));
        contentTypes()
                .forEach(contentType -> modelBuilder.add(API.hasContentType, Values.literal(contentType)));
        includeSubscriptionEventType()
                .forEach(type -> modelBuilder.add("api:includeSubscriptionEventType", type));
        Model model = modelBuilder.build();
        return new LogisticsObject(subscriptionIRI, RDFUtil.getType(model, subscriptionIRI), model);
    }

    public static List<IRI> includeSubscriptionEventType() {
        return List.of(API.LOGISTICS_EVENT_RECEIVED,
                API.LOGISTICS_OBJECT_UPDATED,
                API.LOGISTICS_OBJECT_CREATED);
    }

    public static List<String> contentTypes() {
        return List.of(APPLICATION_JSON_LD_VALUE
//                ,
//                APPLICATION_TURTLE_VALUE,
//                TEXT_TURTLE_VALUE
        );
    }

    public void processSubscriptionRequest(LogisticsObject subscription, IRI status) {
        log.debug("processSubscriptionRequest is invoked.");
        IRI topicIRI = Models.objectIRI(subscription.model().filter(subscription.iri(), API.hasTopic, null))
                .orElse(null);
        log.debug("Subscription Topic :{}", topicIRI);
        if (topicIRI == null) {
            return;
        }
        NotificationEventType notificationEventType = getNotificationEventType(status);
        // create and publish event.
        logisticsEventService.addEventToLogisticsObject(topicIRI, notificationEventType);
    }

    private static NotificationEventType getNotificationEventType(IRI status) {
        if (status.equals(API.REQUEST_ACCEPTED)) {
            return NotificationEventType.SUBSCRIPTION_REQUEST_ACCEPTED;
        } else if (status.equals(API.REQUEST_REJECTED)) {
            return NotificationEventType.SUBSCRIPTION_REQUEST_REJECTED;
        } else if (status.equals(API.REQUEST_REVOKED)) {
            return NotificationEventType.SUBSCRIPTION_REQUEST_REVOKED;
        } else if (status.equals(API.REQUEST_FAILED)) {
            return NotificationEventType.SUBSCRIPTION_REQUEST_FAILED;
        }
        return NotificationEventType.SUBSCRIPTION_REQUEST_PENDING;
    }
}
