package io.ontomesh.onerecord.server.services.logisticsobjects.events;

import io.ontomesh.core.rdf.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.api.enums.EventStatus;
import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.domain.LogisticsEvents;
import io.ontomesh.onerecord.server.event.EventProducer;
import io.ontomesh.onerecord.server.event.api.Producer;
import io.ontomesh.onerecord.server.exception.LinkedObjectNotMatchException;
import io.ontomesh.onerecord.server.exception.LinkedObjectReferenceMissingException;
import io.ontomesh.onerecord.server.exception.LogisticsEventNotFoundException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import io.ontomesh.onerecord.server.services.logisticsobjects.AbstractLogisticsObjectService;
import io.ontomesh.onerecord.server.services.server.ServerInformationService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static io.ontomesh.onerecord.server.api.enums.EventStatus.FAILED;
import static io.ontomesh.onerecord.server.api.enums.EventStatus.PUBLISHED;

@Slf4j
@Service
public class LogisticsEventService extends AbstractLogisticsObjectService {

    public static final IRI oldValue = OntoMesh.hasEventStatusNew;
    private final Producer eventProducer;
    private final ServerInformationService serverInformationService;

    /**
     * constructor
     *
     * @param repository    DB repository.
     * @param idProvider    ID provider.
     * @param eventProducer An event producer.
     */
    protected LogisticsEventService(LogisticsObjectRepository repository,
                                    IdProvider idProvider, EventProducer eventProducer,
                                    ServerInformationService serverInformationService) {
        super(repository, idProvider);
        this.eventProducer = eventProducer;
        this.serverInformationService = serverInformationService;
    }


    /**
     * Validates the logistics event by checking its linked object and matching it with the logistics object.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param event              Logistics Event.
     */
    private void validateEvent(IRI logisticsObjectIRI, LogisticsEvent event) {
        validate(logisticsObjectIRI);

        // Extract the linked object ID from the event model.
        IRI linkedObjectId = event.model().filter(event.iri(), OntoMesh.hasEventStatus, null)
                .objects()
                .stream()
                .findFirst()
                .filter(o -> o instanceof IRI)
                .map(o -> (IRI) o)
                .orElseThrow(() -> new LinkedObjectReferenceMissingException("Linked Object Reference is missing."));

        // Verify that the linked object matches the logistics object.
        if (!linkedObjectId.equals(logisticsObjectIRI)) {
            throw new LinkedObjectNotMatchException(linkedObjectId, "Linked Object reference does not match Logistics Object ID.");
        }
    }


    /**
     * Adds a logistics event to the logistics object and optionally links it.
     *
     * @param loId           Logistics Object ID.
     * @param eventType      Type of notification event.
     * @param logisticsEvent The logistics event model (can be null).
     * @param tenant         Tenant IRI.
     * @param link           Whether to link the event to the logistics object.
     */

    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType, LogisticsEvent logisticsEvent,
                                          IRI tenant, boolean link) {
        log.debug("Adding {} event to logistics object {} ", eventType.label(), loId);
        // Create event if not provided.
        logisticsEvent = Optional.ofNullable(logisticsEvent)
                .orElseGet(() -> createEventModel(loId, eventType, tenant));

        // Save the event model to the repository.
        persist(logisticsEvent);

        // Link the event to the logistics object if specified.
        if (link) {
            linkToLogisticsObject(loId, logisticsEvent.iri());
        }

        // Publish the event to middleware.
        publishLogisticsEvent(logisticsEvent, tenant);
    }

    /**
     * Links a logistics event to a logistics object by adding the relationship to the repository.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param eventIRI           Event IRI.
     */
    private void linkToLogisticsObject(IRI logisticsObjectIRI, IRI eventIRI) {
        getRepository().add(logisticsObjectIRI, OntoMesh.hasLogisticsEvents, eventIRI);
    }

    /**
     * Persists the logistics event model to the repository.
     *
     * @param event Logistics event to persist.
     */
    private void persist(LogisticsEvent event) {
        getRepository().persist(event.model());
    }

    /**
     * Validates if a logistics object exists in the repository.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     */
    private void validate(IRI logisticsObjectIRI) {
        if (!getRepository().findLoById(logisticsObjectIRI).isPresent()) {
            throw new LogisticsObjectNotFoundException(logisticsObjectIRI, "Logistics Object not found.");
        }
    }

    /**
     * Overloaded method for adding a logistics event without specifying a model or linking.
     *
     * @param loId      Logistics Object ID.
     * @param eventType Type of notification event.
     */
    @Transactional
    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType) {
        addEventToLogisticsObject(loId, eventType, null, serverInformationService.getTenant(), false);
    }

    /**
     * @param loId
     * @param eventType
     * @param logisticsEvent
     */
    @Transactional
    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType, LogisticsEvent logisticsEvent) {
        addEventToLogisticsObject(loId, eventType, logisticsEvent, serverInformationService.getTenant(), false);
    }

    /**
     * @param loId
     * @param eventType
     * @param logisticsEvent
     * @param link
     */
    @Transactional
    public void addEventToLogisticsObject(IRI loId, NotificationEventType eventType, LogisticsEvent logisticsEvent,
                                          boolean link) {
        addEventToLogisticsObject(loId, eventType, logisticsEvent, serverInformationService.getTenant(), link);
    }


    /**
     * Publishes the logistics event to the middleware and updates its status in the repository.
     *
     * @param logisticsEvent Logistics event to publish.
     * @param tenant         Tenant IRI.
     */
    private void publishLogisticsEvent(LogisticsEvent logisticsEvent, IRI tenant) {
        // Convert the event model to a JSON-LD string.
        String eventString = new ModelToStringConverter(RDFFormat.JSONLD).convert(logisticsEvent.model());

        // Send the event to the producer and update its status based on the outcome.
        eventProducer.send(logisticsEvent.iri().stringValue(), eventString, tenant, (result, throwable) -> {
            EventStatus eventStatus = (throwable == null) ? PUBLISHED : FAILED;
            getRepository().update(logisticsEvent.iri(), OntoMesh.hasEventStatus, OntoMesh.hasEventStatusNew, eventStatus.label());
        });
    }

    /**
     * Creates a logistics event model for a given logistics object ID, event type, and tenant.
     *
     * @param loId      The IRI of the logistics object.
     * @param eventType The type of notification event.
     * @param tenant    The IRI of the tenant.
     * @return The created LogisticsEvent.
     */
    public LogisticsEvent createEventModel(IRI loId, NotificationEventType eventType, IRI tenant) {
        IdProvider idProvider = getIdProvider();
        OntoMeshId eventId = idProvider.createUniqueLogisticsEvent(loId);
        IRI eventCodeIRI = idProvider.createInternalIri();

        LocalDateTime now = LocalDateTime.now();

        // Build model for event
        //@formatter:off
        ModelBuilder builder = new ModelBuilder()
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(eventId.iri())
                    .add(RDF.TYPE, CARGO.LogisticsEvent)
                    .add(CARGO.creationDate, now)
                    .add(CARGO.eventDate, now)
                    .add(CARGO.eventName, eventType.name())
                    .add(CARGO.partialEventIndicator, false)
                    .add(CARGO.eventFor, loId)
                    .add(CARGO.recordingOrganization, tenant)
                    .add(OntoMesh.isInternal, true)
                    .add(CARGO.eventCode, eventCodeIRI)
                    .add(CARGO.eventTimeType, CARGO.ACTUAL)
                .subject(loId)
                    .add(RDF.TYPE, super.getType(loId))
                .subject(eventCodeIRI)
                    .add(RDF.TYPE, CARGO.CodeListElement)
                    .add(CARGO.code, eventType.label().stringValue())
                    .add(CARGO.codeListName, eventType.getCodeListName())
                .subject(CARGO.ACTUAL)
                    .add(RDF.TYPE, CARGO.EventTimeType);
        //@formatter:on
        // Build and return the event model
        return new LogisticsEvent(eventId.iri(), builder.build());
    }


    /**
     * Saves a logistics event and links it to the logistics object.
     *
     * @param logisticsObjectIRI Logistics Object IRI.
     * @param event              Logistics Event to save.
     * @return The IRI of the saved event.
     */
    public IRI saveLogisticsEvent(IRI logisticsObjectIRI, LogisticsEvent event) {
        // Validate the logistics object and event.
        validateEvent(logisticsObjectIRI, event);

        // Persist the event and link it to the logistics object.
        persist(event);
        linkToLogisticsObject(logisticsObjectIRI, event.iri());

        return event.iri();
    }

    /**
     * Retrieves logistics events associated with a given logistics object, filtered by event type.
     *
     * @param logisticsObjectIRI IRI of the logistics object.
     * @param eventType          Type of the logistics events to filter.
     * @return A collection of logistics events wrapped in a LogisticsEvents object.
     */
    public LogisticsEvents getEventsByLogisticsObjectIRI(IRI logisticsObjectIRI, String eventType) {

        IRI id = getIdProvider().getLogisticsEventId(logisticsObjectIRI).iri();
        ModelBuilder builder = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(id)
                .add(RDF.TYPE, OntoMesh.Collection);

        AtomicInteger counter = new AtomicInteger();
        List<Statement> statements = this.getRepository()
                .getStatements(logisticsObjectIRI, OntoMesh.hasLogisticsEvents, null)
                .stream()
                .filter(statement -> statement.getObject().isIRI())
                .toList(); // Collect to avoid repeatedly filtering in the loop

        statements.forEach(statement -> {
            IRI logisticsEventIRI = (IRI) statement.getObject();
            log.debug("Logistics Event IRI {}", logisticsEventIRI);

            LogisticsEvent logisticsEvent = getLogisticsEvent(logisticsEventIRI);

            // Check if the event type matches, if specified
            if (StringUtils.hasText(eventType) && !eventType.equals(logisticsEvent.getLogisticsEventCode())) {
                return; // Skip if event types do not match
            }

            counter.incrementAndGet(); // Increment counter atomically

            builder.add(OntoMesh.hasItems, logisticsEventIRI);
            builder.subject(logisticsEventIRI);

            // Add logistics event statements in bulk to avoid multiple method calls
            logisticsEvent.model().forEach(eventStatement ->
                    builder.add(eventStatement.getSubject(), eventStatement.getPredicate(), eventStatement.getObject())
            );

            builder.subject(id.toString()); // Set subject as id
        });

        builder.add(id.toString(), OntoMesh.hasTotalItems, counter);
        return new LogisticsEvents(Values.iri(id.toString()), builder.build());
    }


    /**
     * Retrieves the Logistics Event.
     *
     * @param logisticEventIRI Logistics Event IRI.
     * @return Logistics Event
     */
    public LogisticsEvent getLogisticsEvent(IRI logisticEventIRI) {
        Model model = getRepository().findEventModelById(logisticEventIRI);
        if (model.isEmpty()) {
            throw new LogisticsEventNotFoundException(logisticEventIRI);
        }
        return new LogisticsEvent(logisticEventIRI, model);
    }

}
