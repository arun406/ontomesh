package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.enums.EventStatus;
import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.event.EventProducer;
import io.ontomesh.onerecord.server.exception.LinkedObjectNotMatchException;
import io.ontomesh.onerecord.server.exception.LinkedObjectReferenceMissingException;
import io.ontomesh.onerecord.server.exception.LogisticsEventNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Slf4j
@Service
public class LogisticsEventService extends AbstractLogisticsObjectService {
    private final EventProducer eventProducer;

    /**
     * constructor
     *
     * @param repository
     * @param idProvider
     * @param eventProducer
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
        this.repository.add(logisticsObjectIRI, OntoMesh.hasLogisticsEvents, eventIRI);
    }


    /**
     * Persists the Model.
     *
     * @param model model tobe persisted in the database.
     */
    private void saveEvent(Model model) {
        this.repository.persist(model);
    }

    /**
     * Validates the Logistics Object.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param event              Logistics Event.
     */
    private void validate(IRI logisticsObjectIRI, LogisticsObject event) {

        // Check existence of LogisticsObject.
        validate(logisticsObjectIRI);

        Model model = event.model().filter(event.iri(), CARGO.eventFor, null);
        IRI linkedObjectId = Models.objectIRI(model).orElse(null);
        if (linkedObjectId == null) {
            throw new LinkedObjectReferenceMissingException("Linked Object Reference is missing.");
        }
        log.debug("linked object id: {} ", linkedObjectId);
        if (!linkedObjectId.stringValue().equals(logisticsObjectIRI.stringValue())) {
            throw new LinkedObjectNotMatchException("Logistics Object Id and Linked Object reference is not matching.");
        }
    }

    /**
     * Check existence of LogisticsObject.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     */
    private void validate(IRI logisticsObjectIRI) {
        boolean exists = this.repository.findLoById(logisticsObjectIRI).isPresent();
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
    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType) {

        log.debug("Adding {} event to logistics object {} ", eventType.label(), loId);
        OntoMeshId eventId = idProvider.createUniqueLogisticsEvent(loId);
        IRI eventCodeIRI = idProvider.createInternalIri().iri();
        IRI eventForIRI = idProvider.createInternalIri().iri();

        Model eventCodeModel = new ModelBuilder()
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(eventCodeIRI)
                .add(RDF.TYPE, CARGO.CodeListElement)
                .add(CARGO.code, eventType.label())
                .add(CARGO.codeListName, eventType.name())
                .build();

        Model eventTimeTypeModel = new ModelBuilder()
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(CARGO.ACTUAL)
                .add(RDF.TYPE, CARGO.EventTimeType).build();

        this.repository.persist(eventTimeTypeModel);
        this.repository.persist(eventCodeModel);

        Model event = new EventBuilder(eventId.iri(), loId, getType(loId),
                eventType.name(), true, idProvider.tenant(), eventCodeIRI, CARGO.ACTUAL)
                .build();

        // save the LogisticsEvent
        saveEvent(event);

        linkToLogisticsObject(loId, eventId.iri());
        LogisticsObject logisticsEvent = this.getLogisticsEvent(eventId.iri());
        String eventString = new ModelToStringConverter(RDFFormat.TURTLE).convert(logisticsEvent.model());
        // publish the logistics event

        BiConsumer<SendResult<String, String>, Throwable> action = (SendResult<String, String> result, Throwable ex) -> {
            if (ex == null) {
                log.debug("Sent event =[{}] with offset=[{}]",
                        eventId.iri().stringValue(), result.getRecordMetadata().offset());
                LogisticsEventService.this.repository.delete(eventId.iri(), OntoMesh.hasEventStatus, OntoMesh.hasEventStatusNew);
                // update the event status.
                LogisticsEventService.this.repository.add(eventId.iri(), OntoMesh.hasEventStatus, EventStatus.PUBLISHED.label());
            } else {
                log.debug("Unable to send event=[{}}] due to {}: ",
                        eventId.iri().stringValue(), ex.getMessage());
                LogisticsEventService.this.repository.delete(eventId.iri(), OntoMesh.hasEventStatus, OntoMesh.hasEventStatusNew);
                // update the event status.
                LogisticsEventService.this.repository.add(eventId.iri(), OntoMesh.hasEventStatus, EventStatus.FAILED.label());
            }
        };

        this.eventProducer.send(eventString, idProvider.tenant(), action);
    }

    /**
     * Saves The Logistics Event and Links to Logistics Object.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param loType
     * @param event              Logistics Event.
     * @return
     */
    public IRI saveLogisticsEvent(IRI logisticsObjectIRI, IRI loType, LogisticsObject event) {

        // validate.
        validate(logisticsObjectIRI, event);

        // save the model.
        saveEvent(event.model());

        linkToLogisticsObject(logisticsObjectIRI, event.iri());

        addEventToLogisticsObject(logisticsObjectIRI, NotificationEventType.LOGISTICS_EVENT_RECEIVED);

        return event.iri();
    }

    public LogisticsObject getEventsByLogisticsObjectIRI(IRI logisticsObjectIRI, URI id, String eventType) {

        // validate logistics object.
        ModelBuilder builder = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(id.toString())
                .add(RDF.TYPE, OntoMesh.Collection);
        RepositoryResult<Statement> repositoryResult = this.repository
                .getStatements(logisticsObjectIRI, OntoMesh.hasLogisticsEvents, null);
        List<Statement> statementList = repositoryResult.stream().toList();
        int counter = 0;
        for (Statement statement : statementList) {
            if (statement.getObject().isIRI()) {
                log.debug("Logistics Event IRI {}", statement.getObject());
                LogisticsObject logisticsEvent = getLogisticsEvent((IRI) statement.getObject());
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
        return new LogisticsObject(Values.iri(id.toString()), OntoMesh.Collection, model);
    }

    /**
     * Retrieves the Logistics Event.
     *
     * @param logisticEventIRI Logistics Event IRI.
     * @return Logistics Event
     */
    public LogisticsObject getLogisticsEvent(IRI logisticEventIRI) {

        Predicate<Statement> predicate = statement -> !(statement.getPredicate().equals(CARGO.eventFor));

        Optional<LogisticsObject> logisticsObjectOptional = this.repository
                .findLoById(logisticEventIRI, predicate);

        if (logisticsObjectOptional.isEmpty()) {
            throw new LogisticsEventNotFoundException("Logistics events not found.");
        }
        return logisticsObjectOptional.get();
    }

}
