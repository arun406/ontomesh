package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.enums.EventStatus;
import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.event.EventProducer;
import io.ontomesh.onerecord.server.exception.LinkedObjectNotMatchException;
import io.ontomesh.onerecord.server.exception.LinkedObjectReferenceMissingException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class LogisticsEventService extends AbstractLogisticsObjectService {
    private final EventProducer eventProducer;

    /**
     * constructor
     *
     * @param repository    DB repository.
     * @param idProvider    ID provider.
     * @param eventProducer An event producer.
     */
    protected LogisticsEventService(LogisticsObjectRepository repository,
                                    IdProvider idProvider, EventProducer eventProducer) {
        super(repository, idProvider);
        this.eventProducer = eventProducer;
    }

    private boolean getLogisticsEventType(IRI iri, Model model, String eventType) {
        return !model.filter(iri, CARGO.eventCode, Values.literal(eventType)).isEmpty();
    }

    /**
     * Links event to logistics object.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param eventIRI           event IRI.
     */
    private void linkToLogisticsObject(IRI logisticsObjectIRI, IRI eventIRI) {
        getRepository().add(logisticsObjectIRI, OntoMesh.hasLogisticsEvents, eventIRI);
    }


    /**
     * Persists the Model.
     *
     * @param event model tobe persisted in the database.
     */
    private void saveEvent(LogisticsEvent event) {
        getRepository().persist(event.model());
    }

    /**
     * Validates the Logistics Object.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param event              Logistics Event.
     */
    private void validate(IRI logisticsObjectIRI, LogisticsEvent event) {

        // Check existence of LogisticsObject.
        validate(logisticsObjectIRI);

        Model model = event.model().filter(event.iri(), CARGO.eventFor, null);
        IRI linkedObjectId = Models.objectIRI(model).orElse(null);
        if (linkedObjectId == null) {
            throw new LinkedObjectReferenceMissingException("Linked Object Reference is missing.");
        }
        log.debug("linked object id: {} ", linkedObjectId);
        if (!linkedObjectId.equals(logisticsObjectIRI)) {
            throw new LinkedObjectNotMatchException("Logistics Object Id and Linked Object reference is not matching.");
        }
    }

    /**
     * Check existence of LogisticsObject.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     */
    private void validate(IRI logisticsObjectIRI) {
        boolean exists = getRepository().findLoById(logisticsObjectIRI).isPresent();
        if (!exists) {
            throw new LogisticsObjectNotFoundException(logisticsObjectIRI,
                    "Logistics Object with the specified URI could not be found in the database.");
        }
    }

    /**
     * saves the logistics event and also publishes the event to middleware.
     *
     * @param loId      logistics object id.
     * @param eventType notification event type.
     */
    @Transactional
    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType, LogisticsEvent logisticsEvent,
                                          IRI tenant, boolean link) {
        log.debug("Adding {} event to logistics object {} ", eventType.label(), loId);
        if (logisticsEvent == null) {
            logisticsEvent = createEventModel(loId, eventType, tenant);
        }
        // save the LogisticsEvent
        saveEvent(logisticsEvent);
        if (link) {
            linkToLogisticsObject(loId, logisticsEvent.iri());
        }
        publishLogisticsEvent(logisticsEvent, tenant);
    }


    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType) {
        addEventToLogisticsObject(loId, eventType, null, getTenant(), false);
    }

    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType, LogisticsEvent logisticsEvent) {
        addEventToLogisticsObject(loId, eventType, logisticsEvent, getTenant(), false);
    }

    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType, LogisticsEvent logisticsEvent, boolean link) {
        addEventToLogisticsObject(loId, eventType, logisticsEvent, getTenant(), link);
    }

    private void publishLogisticsEvent(LogisticsEvent logisticsEvent, IRI tenant) {
        String eventString = new ModelToStringConverter(RDFFormat.TURTLE).convert(logisticsEvent.model());

        // publish the logistics event
        BiConsumer<SendResult<String, String>, Throwable> action = (SendResult<String, String> result, Throwable ex) -> {
            if (ex == null) {
                log.debug("Sent event =[{}] with offset=[{}]", logisticsEvent.iri().stringValue(), result.getRecordMetadata().offset());
                // update the event status.
                getRepository().update(logisticsEvent.iri(), OntoMesh.hasEventStatus, OntoMesh.hasEventStatusNew, EventStatus.PUBLISHED.label());
            } else {
                log.debug("Unable to send event=[{}}] due to {}: ", logisticsEvent.iri().stringValue(), ex.getMessage());
                // update the event status.
                getRepository().update(logisticsEvent.iri(), OntoMesh.hasEventStatus, OntoMesh.hasEventStatusNew, EventStatus.FAILED.label());
            }
        };

        this.eventProducer.send(eventString, tenant, action);
    }

    public LogisticsEvent createEventModel(IRI loId, NotificationEventType eventType, IRI tenant) {
        IdProvider idProvider = getIdProvider();
        OntoMeshId eventId = idProvider.createUniqueLogisticsEvent(loId);
        IRI eventCodeIRI = idProvider.createInternalIri().iri();
        ModelBuilder builder = new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(eventId.iri())
                .add(RDF.TYPE, CARGO.LogisticsEvent)
                .add(CARGO.creationDate, LocalDateTime.now())
                .add(CARGO.eventDate, LocalDateTime.now())//TODO : revisit
                .add(CARGO.eventName, eventType.name())
                .add(CARGO.partialEventIndicator, false)
                .add(CARGO.eventFor, loId)
                .add(CARGO.recordingOrganization, tenant)
                .add(OntoMesh.isInternal, Values.literal(true))
                .add(CARGO.eventCode, eventCodeIRI)
                .subject(eventCodeIRI)  //switch to eventCode
                .add(RDF.TYPE, CARGO.CodeListElement)
                .add(CARGO.code, eventType.label().stringValue())
                .add(CARGO.codeListName, eventType.getCodeListName())
                .subject(eventId.iri())
                .add(CARGO.eventTimeType, CARGO.ACTUAL)
                .subject(CARGO.ACTUAL)
                .add(RDF.TYPE, CARGO.EventTimeType);

        Model model = builder.build();

        return new LogisticsEvent(eventId.iri(), model);
    }

    /**
     * Saves The Logistics Event and Links to Logistics Object.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param event              Logistics Event.
     * @return
     */
    public IRI saveLogisticsEvent(IRI logisticsObjectIRI, LogisticsEvent event) {

        // validate.
        validate(logisticsObjectIRI, event);
        // save the model.
        saveEvent(event);
//        linkToLogisticsObject(logisticsObjectIRI, event.iri());
        addEventToLogisticsObject(logisticsObjectIRI, NotificationEventType.LOGISTICS_EVENT_RECEIVED, event, true);
        return event.iri();
    }

    public LogisticsEvent getEventsByLogisticsObjectIRI(IRI logisticsObjectIRI, URI id, String eventType) {

        // validate logistics object.
        ModelBuilder builder = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(id.toString())
                .add(RDF.TYPE, OntoMesh.Collection);
        RepositoryResult<Statement> repositoryResult = this.getRepository()
                .getStatements(logisticsObjectIRI, OntoMesh.hasLogisticsEvents, null);
        List<Statement> statementList = repositoryResult.stream().toList();
        int counter = 0;
        for (Statement statement : statementList) {
            if (statement.getObject().isIRI()) {
                log.debug("Logistics Event IRI {}", statement.getObject());
                LogisticsEvent logisticsEvent = getLogisticsEvent((IRI) statement.getObject());
                if (StringUtils.hasText(eventType) &&
                        !getLogisticsEventType(logisticsEvent.iri(), logisticsEvent.model(), eventType)) {
                    continue;
                }
                counter++;
                builder.add(OntoMesh.hasItems, statement.getObject());
                builder.subject((Resource) statement.getObject());
                for (Statement eventStatement : logisticsEvent.model()) {
                    builder.add(eventStatement.getSubject(), eventStatement.getPredicate(), eventStatement.getObject());
                }
                builder.subject(id.toString());
            }
        }
        builder.add(id.toString(), OntoMesh.hasTotalItems, counter);
        Model model = builder.build();
//        return new LogisticsEvent(Values.iri(id.toString()), OntoMesh.Collection, model);
        return new LogisticsEvent(Values.iri(id.toString()), model);
    }

    /**
     * Retrieves the Logistics Event.
     *
     * @param logisticEventIRI Logistics Event IRI.
     * @return Logistics Event
     */
    public LogisticsEvent getLogisticsEvent(IRI logisticEventIRI) {

//        Predicate<Statement> predicate = statement -> !(statement.getPredicate().equals(CARGO.eventFor));
//
//        Optional<LogisticsEvent> logisticsObjectOptional = this.getRepository()
//                .findEventsByLogisticsObjectId(logisticEventIRI, predicate);

//        if (logisticsObjectOptional.isEmpty()) {
//            throw new LogisticsEventNotFoundException("Logistics events not found.");
//        }

        Model model = getRepository().findEventModelById(logisticEventIRI);

        return new LogisticsEvent(logisticEventIRI, model);
    }

}
