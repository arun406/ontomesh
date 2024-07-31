package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsSubscriptionNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;

@Slf4j
@Service
public class SubscriptionService extends AbstractLogisticsObjectService {

    public static final Map<IRI, NotificationEventType> notificationEventTypeMap = new HashMap<>() {{
        put(API.REQUEST_ACCEPTED, NotificationEventType.SUBSCRIPTION_REQUEST_ACCEPTED);
        put(API.REQUEST_REJECTED, NotificationEventType.SUBSCRIPTION_REQUEST_REJECTED);
        put(API.REQUEST_REVOKED, NotificationEventType.SUBSCRIPTION_REQUEST_REVOKED);
        put(API.REQUEST_FAILED, NotificationEventType.SUBSCRIPTION_REQUEST_FAILED);
    }};

    private final LogisticsEventService logisticsEventService;

    @Value("${onerecord.server.subscriptions.send-logistics-object-body:false}")
    private boolean sendLogisticsObjectBody;

    @Value("${onerecord.server.subscriptions.subscribe-to-logistics-events:true}")
    private boolean subscribeToLogisticsEvents;

    @Value("${onerecord.server.subscriptions.notify-request-status-change:true}")
    private boolean notifyRequestStatusChange;

    @Value("${onerecord.server.subscriptions.include-subscription-event-type:}")
    private String[] includeSubscriptionEventType;

    public SubscriptionService(LogisticsObjectRepository repository, IdProvider idProvider,
                               LogisticsEventService logisticsEventService) {
        super(repository, idProvider);
        this.logisticsEventService = logisticsEventService;
    }

    private static IRI getTopicTypeIRI(Model model, IRI iri) {
        return Models.objectIRI(model.filter(iri, API.hasTopicType, null))
                .orElse(null);
    }

    private List<String> contentTypes() {
        return List.of(APPLICATION_JSON_LD_VALUE);
    }


    public static NotificationEventType getNotificationEventType(IRI status) {
        return notificationEventTypeMap.getOrDefault(status, NotificationEventType.SUBSCRIPTION_REQUEST_PENDING);
    }

    public static IRI getTopicIRI(Model model, IRI iri) {

        IRI topicTypeIRI = getTopicTypeIRI(model, iri);

        if (topicTypeIRI == null) {
            log.warn("Subscription Topic Type is not available.");
            return null;
        }
        if (topicTypeIRI.equals(API.LOGISTICS_OBJECT_IDENTIFIER)) {
            IRI topicIRI = Models.objectIRI(model.filter(iri, API.hasTopic, null))
                    .orElse(null);

            if (topicIRI == null) {
                log.warn("Subscription Topic is not available.");
                return null;
            }
            return topicIRI;
        }
        return null;
    }

    /**
     * Returns the supported logistics objects type by the one record server.
     *
     * @return List of IRI
     */
    public List<IRI> includeSubscriptionEventType() {
        return Stream.of(includeSubscriptionEventType)
                .map(Values::iri)
                .collect(Collectors.toList());
    }

    /**
     * Creates and returns the Subscription (proposal).
     *
     * @param topic     Topic.
     * @param topicType Topic Type.
     * @return Subscription Logistics Object.
     */
    public LogisticsObject createSubscriptionProposalResponse(IRI topic, IRI topicType) {

        IRI subscriptionIRI = getIdProvider().createUniqueSubscriptionUri().iri();
        ModelBuilder modelBuilder = new ModelBuilder()
                .subject(subscriptionIRI)
                .add(RDF.TYPE, API.Subscription)
                .add(API.hasSubscriber, getIdProvider().tenant())
                .add(API.hasTopic, topic)
                .add(API.hasTopicType, topicType)
                .add(API.notifyRequestStatusChange, Values.literal(notifyRequestStatusChange))
                .add(API.expiresAt, Values.literal(Instant.now().toString(), CoreDatatype.XSD.DATETIME))
                .add(API.sendLogisticsObjectBody, Values.literal(sendLogisticsObjectBody));

        contentTypes()
                .forEach(contentType -> modelBuilder.add(API.hasContentType, Values.literal(contentType)));

        includeSubscriptionEventType()
                .forEach(type -> modelBuilder.add(API.includeSubscriptionEventType, type));
        Model model = modelBuilder.build();

        return new LogisticsObject(subscriptionIRI, RDFUtil.getType(model, subscriptionIRI), model);
    }

    /**
     * Process the Subscription Request.
     *
     * @param subscription subscription
     * @param status       subscription status
     */
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

    /**
     * returns the subscriptions for Logistics Object Type
     *
     * @param loType Logistics Object Type.
     * @return Logistics Object
     */
    public LogisticsObject getSubscriptionsByLoType(IRI loType, IRI eventType) {

        final String subject = STR."\{loType.stringValue()}/subscriptions";

        ModelBuilder builder = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(subject)
                .add(RDF.TYPE, OntoMesh.Collection);

        RepositoryResult<Statement> repositoryResult = this.getRepository()
                .getStatements(null, API.hasTopic, loType);

        List<Statement> statementList = repositoryResult.stream().toList();

        int counter = 0;
        for (Statement statement : statementList) {
            if (!statement.getObject().isIRI()) {
                continue;
            }
            log.debug("Logistics Subscription IRI {}", statement.getObject());

            LogisticsObject subscription = getSubscription((IRI) statement.getSubject());
            Model statements = subscription.model()
                    .filter(subscription.iri(), API.includeSubscriptionEventType, eventType);
            if (statements.isEmpty()) {
                continue;
            }
            counter++;
            builder.add(OntoMesh.hasItems, statement.getSubject());
            builder.subject(statement.getSubject());
            for (Statement ss : subscription.model()) {
                builder
                        .add(ss.getSubject(), ss.getPredicate(), ss.getObject());
            }
            builder.subject(subject);
        }
        builder.add(subject, OntoMesh.hasTotalItems, counter);
        Model model = builder.build();
        return new LogisticsObject(Values.iri(subject), OntoMesh.Collection, model);
    }

    /**
     * returns the subscriptions for Logistics Object.
     *
     * @param logisticsObjectIRI Logistics Object Id.
     * @return collection of Subscription packed as LogisticsObject.
     */
    public LogisticsObject getSubscriptionsByLoId(IRI logisticsObjectIRI) {

        final String subject = STR."\{logisticsObjectIRI.stringValue()}/subscriptions";
        // validate logistics object.
        ModelBuilder builder = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(subject)
                .add(RDF.TYPE, OntoMesh.Collection);

        RepositoryResult<Statement> repositoryResult = this.getRepository()
                .getStatements(null, API.hasTopic, logisticsObjectIRI);

        List<Statement> statementList = repositoryResult.stream().toList();

        int counter = 0;
        for (Statement statement : statementList) {
            if (statement.getObject().isIRI()) {

                LogisticsObject subscription = getSubscription((IRI) statement.getSubject());
                counter++;
                builder.add(OntoMesh.hasItems, statement.getSubject());
                builder.subject(statement.getSubject());
                for (Statement ss : subscription.model()) {
                    builder
                            .add(ss.getSubject(), ss.getPredicate(), ss.getObject());
                }
                builder.subject(subject);
            }
        }
        builder.add(subject, OntoMesh.hasTotalItems, counter);
        Model model = builder.build();
        return new LogisticsObject(Values.iri(subject), OntoMesh.Collection, model);
    }

    /**
     * Fetches the Subscription.
     *
     * @param subscriptionIRI
     * @return
     */
    public LogisticsObject getSubscription(IRI subscriptionIRI) {

        Optional<LogisticsObject> logisticsObjectOptional = this.getRepository()
                .findLoById(subscriptionIRI, OntoMeshId::isInternalIri);

        return logisticsObjectOptional
                .orElseThrow(() -> new LogisticsSubscriptionNotFoundException("Logistics subscription not found."));
    }


    public List<IRI> getSubscribersForLoId(IRI loId) {
        List<IRI> subscribers = new ArrayList<>();

        TupleQuery tupleQuery = this.getRepository().getConnection().prepareTupleQuery(STR
                ."""
                    PREFIX api:  <https://onerecord.iata.org/ns/api#>

                    SELECT  ?subscriber
                    WHERE
                    {
                      ?actionrequest <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> api:SubscriptionRequest .
                      ?actionrequest api:hasSubscription ?subscription .
                      ?subscription api:hasTopic ?logisticsobject .
                      ?subscription api:hasSubscriber ?subscriber .
                      FILTER(?logisticsobject = <\{loId.stringValue()}>)
                                    }
                """);
//        tupleQuery.setBinding(logisticsObject.getVarName(), loId);
        TupleQueryResult evaluate = tupleQuery.evaluate();
        while (evaluate.hasNext()) {
            BindingSet next = evaluate.next();
            for (Binding binding : next) {
                subscribers.add((IRI) binding.getValue());
            }
        }

        return subscribers;
    }

    public List<IRI> getSubscribers(IRI hasTopic, IRI hasTopicType) {
        List<IRI> subscribers = new ArrayList<>();

        String tupleQueryString = STR."""
                                PREFIX api:  <https://onerecord.iata.org/ns/api#>

                                SELECT  ?subscriber
                                WHERE
                                {
                                  ?actionrequest <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> api:SubscriptionRequest> .
                                  ?actionrequest api:hasSubscription ?subscription .
                                  ?subscription api:hasTopic ?logisticsobject .
                                  ?subscription api:hasTopicType  ?topicType  .
                                  ?subscription api:hasSubscriber ?subscriber .
                                  FILTER(?logisticsobject = <\{hasTopic.stringValue()}> && ?topicType = <\{hasTopicType.stringValue()}> )
                                }
                                """;

        log.info("tuple query {}", tupleQueryString);
        TupleQuery tupleQuery = this.getRepository().getConnection().prepareTupleQuery(tupleQueryString);
        TupleQueryResult evaluate = tupleQuery.evaluate();
        while (evaluate.hasNext()) {
            BindingSet next = evaluate.next();
            for (Binding binding : next) {
                log.info("name: {}, value : {} ", binding.getName(), binding.getValue());
                subscribers.add((IRI) binding.getValue());
            }
        }

        return subscribers;
    }
}
