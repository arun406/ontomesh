package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.InvalidChangeOperationException;
import io.ontomesh.onerecord.server.exception.InvalidRevisionException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.ontomesh.onerecord.server.util.RDFUtil.getObject;
import static io.ontomesh.onerecord.server.util.RDFUtil.getObjectLiteral;

@Slf4j
@Service
public class LogisticsObjectService extends AbstractLogisticsObjectService {

    private static final Map<IRI, NotificationEventType> notificationEventTypeMap = new HashMap<>() {
        {
            put(API.REQUEST_ACCEPTED, NotificationEventType.CHANGE_REQUEST_ACCEPTED);
            put(API.REQUEST_REJECTED, NotificationEventType.CHANGE_REQUEST_REJECTED);
            put(API.REQUEST_REVOKED, NotificationEventType.CHANGE_REQUEST_REVOKED);
            put(API.REQUEST_FAILED, NotificationEventType.CHANGE_REQUEST_FAILED);
        }
    };
    private final SnapshotService snapshotService;
    private final LogisticsEventService eventService;

    public LogisticsObjectService(LogisticsObjectRepository repository, IdProvider idProvider, SnapshotService snapshotService, LogisticsEventService eventService) {
        super(repository, idProvider);
        this.snapshotService = snapshotService;
        this.eventService = eventService;
    }

    public static NotificationEventType getNotificationEventType(IRI status) {
        return notificationEventTypeMap.getOrDefault(status, NotificationEventType.CHANGE_REQUEST_PENDING);
    }

    private Map<String, IRI> getBlankNodeToIri(Model changeModel, Set<IRI> operationObjects, String operation) {
        return operationObjects.stream()
                .filter(iri -> {
                    IRI object = getObject(iri, API.op, changeModel);
                    return object.toString().equalsIgnoreCase(operation);
                })
                .map(subject -> getObjectLiteral(subject, API.s, changeModel).stringValue())
                .filter(s -> s.startsWith("_:"))
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::resolveAddOperationIri,
                        (iri, x) -> iri
                ));
    }


    private void processAddOperations(LogisticsObject logisticsObject, Set<IRI> changedProperties, Set<IRI> operationObjects) {
        log.debug("Applying Add operations...");
        Model changeModel = logisticsObject.model();
        Map<String, IRI> blankNodeToIri = getBlankNodeToIri(changeModel, operationObjects, API.ADD.stringValue());

        for (IRI opSubject : operationObjects) {
            IRI patchOperation = getObject(opSubject, API.op, changeModel);
            if (!patchOperation.toString().equalsIgnoreCase(API.ADD.stringValue())) {
                continue;
            }
            IRI predicate = Values.iri(getObjectLiteral(opSubject, API.p, changeModel).stringValue());
            log.debug("Predicate :{} ", predicate);
            if (URI.create(predicate.stringValue()).compareTo(URI.create("https://onerecord.iata.org/ns/cargo#hasLogisticsEvent")) == 0) {
                InvalidChangeOperationException exception = new InvalidChangeOperationException();
                exception.setIri(logisticsObject.iri());
                throw exception;
            }
            String subjectAsString = getObjectLiteral(opSubject, API.s, changeModel).stringValue();
            changedProperties.add(predicate);
            // either the opSubject is an IRI or a blank node that is mapped to an internal IRI
            IRI subject = blankNodeToIri.containsKey(subjectAsString) ? blankNodeToIri.get(subjectAsString) : Values.iri(subjectAsString);

            IRI operationObjectSubject = getObject(opSubject, API.o, changeModel);
            String datatype = getObjectLiteral(operationObjectSubject, API.hasDatatype, changeModel).stringValue();
            Literal objectLiteral = getObjectLiteral(operationObjectSubject, API.hasValue, changeModel);
            String value = objectLiteral.stringValue();

            Optional<XSD.Datatype> literalDatatype = XSD.Datatype.from(Values.iri(datatype));
            Value object;
            if (literalDatatype.isPresent()) {
                // a literal is added
                object = Values.literal(value, literalDatatype.get().getCoreDatatype());
            } else {
                // an "embedded" object is to be added
                IRI objectIri = blankNodeToIri.containsKey(value)
                        ? blankNodeToIri.get(value)
                        : Values.iri(value);
                //  - add type statement using operations datatype value
                getRepository().add(objectIri, RDF.TYPE, Values.iri(datatype));
                //  - set the object to be the IRI of the "embedded object"
                object = objectIri;
            }
            getRepository().add(subject, predicate, object);
        }
    }

    private void processDeleteOperations(LogisticsObject change, Set<IRI> changedProperties, Set<Resource> affectedSubjects, Set<IRI> operationObjects) {
        log.debug("Applying delete operations...");
        Model changeModel = change.model();
        Map<String, IRI> blankNodeToIri = getBlankNodeToIri(changeModel, operationObjects, API.DELETE.stringValue());

        for (IRI operationObject : operationObjects) {
            IRI patchOperation = getObject(operationObject, API.op, changeModel);
            if (!patchOperation.toString().equalsIgnoreCase(API.DELETE.stringValue())) {
                continue;
            }

            IRI predicate = Values.iri(getObjectLiteral(operationObject, API.p, changeModel).stringValue());

            // get the predicate
            changedProperties.add(predicate);

            String subjectAsString = getObjectLiteral(operationObject, API.s, changeModel).stringValue();
//            IRI s = Values.iri(subjectAsString);
            // either the opSubject is an IRI or a blank node that is mapped to an internal IRI
            IRI subject = blankNodeToIri.containsKey(subjectAsString) ? blankNodeToIri.get(subjectAsString) : Values.iri(subjectAsString);
            log.debug("subject: {}", subject);
            // get the subject
            affectedSubjects.add(subject);

            IRI operationObjectSubject = getObject(operationObject, API.o, changeModel);
            String datatype = getObjectLiteral(operationObjectSubject, API.hasDatatype, changeModel).stringValue();
            String value = getObjectLiteral(operationObjectSubject, API.hasValue, changeModel).stringValue();
            log.debug("Operation Object Value: {}", value);
            // either the datatype id a core XSD datatype or an IRI
            Optional<XSD.Datatype> literalDatatype = XSD.Datatype.from(Values.iri(datatype));
            Value object = literalDatatype.isPresent() ? Values.literal(value, literalDatatype.get().getCoreDatatype()) : Values.iri(value);

            // delete the triple
            int deletedTriples = getRepository().delete(subject, predicate, object);

            // if not exactly one triple is deleted throw an exception, this cancels the transaction and
            // nothing is changed
            if (deletedTriples != 1) {
                throw new RuntimeException(String.format("Number of affected triples should be 1 but was %s", deletedTriples));
            }
        }
    }

    private IRI resolveAddOperationIri(String value) {
        IRI iri;
        if (value.startsWith("_:")) {
            iri = getIdProvider().createInternalIri();
        } else {
            iri = Values.iri(value);
        }
        return iri;
    }

    /**
     * Updates the logistics object revision.
     *
     * @param logisticsObject logistics object id
     */
    private Literal updateLogisticObjectRevision(LogisticsObject logisticsObject) {
        return getRepository().incrementLogisticsObjectVersion(logisticsObject.iri(), logisticsObject.revision());
    }


    /**
     * validates the latest logistics object revision with the change request revision.
     *
     * @param change          change request.
     * @param logisticsObject logistics object.
     */
    private void validateRevision(LogisticsObject change, LogisticsObject logisticsObject) {

        log.debug("logistics object iri :{}", logisticsObject.iri());

        Literal changeRevision = change.revision();
        Literal loRevision = logisticsObject.revision();
        if (loRevision.intValue() != changeRevision.intValue()) {
            throw new InvalidRevisionException(logisticsObject.iri());
        }
    }

    @Transactional
    public void saveLogisticsObject(LogisticsObject logisticsObject) {
        validate(logisticsObject);

        // Add Audit: creationDate
        logisticsObject.add(CARGO.creationDate, SimpleValueFactory.getInstance().createLiteral(OffsetDateTime.now(ZoneOffset.UTC)));

        super.saveLogisticsObject(logisticsObject);
        // create and publish event.
        addEventToLogisticsObject(NotificationEventType.LOGISTICS_OBJECT_CREATED, logisticsObject.iri());
    }

    @Transactional
    public boolean isExists(IRI iri) {
        Optional<LogisticsObject> optionalModel = this.getRepository().findLoById(iri);
        return optionalModel.isPresent();
    }

    /**
     * Process the Change Request.
     *
     * @param change          Change request
     * @param logisticsObject base logistics object
     */
    @Transactional
    public void processChangeRequest(LogisticsObject change, LogisticsObject logisticsObject) {

        validateRevision(change, logisticsObject);

        applyChanges(change);

        // increase the Logistics Object revision after successful change
        Literal revision = updateLogisticObjectRevision(logisticsObject);
        logisticsObject.updateRevision(revision.intValue());
        log.debug("latest revision {}", revision);
        // create and publish event.
        addEventToLogisticsObject(NotificationEventType.LOGISTICS_OBJECT_UPDATED, logisticsObject.iri());

        LogisticsObject snapshot = snapshotService.createSnapshot(logisticsObject);
        log.info("Snapshot with {} is created successfully.", snapshot.iri());
    }


    @Transactional
    public Set<IRI> applyChanges(LogisticsObject change) {

        Model changeModel = change.model();
        Set<IRI> changedProperties = new HashSet<>();
        Set<Resource> affectedSubjects = new HashSet<>();

        Set<IRI> operationObjects = RDFUtil.getObjects(change.iri(), API.hasOperation, changeModel);
        processDeleteOperations(change, changedProperties, affectedSubjects, operationObjects);
        processAddOperations(change, changedProperties, operationObjects);

        // check if any subject that was affected by a delete operation now only has the "type" triple left
        affectedSubjects.forEach(resource -> {
            Model model = getRepository().getStatements(resource, null, null).stream()
                    .collect(ModelCollector.toModel());
            if (model.size() == 1 && Models.predicate(model.filter(resource, RDF.TYPE, null)).isPresent()) {
                model.forEach(statement ->
                        getRepository().delete(
                                statement.getSubject(),
                                statement.getPredicate(),
                                statement.getObject()
                        )
                );
            }
        });

        return changedProperties;
    }

    public List<IRI> logisticsObjectTypes() {
        return this.getRepository().logisticsObjectTypes();
    }

    private void addEventToLogisticsObject(NotificationEventType notificationEventType, IRI iri) {
        eventService.addEventToLogisticsObject(iri, notificationEventType);
    }

    public List<IRI> listAllLogisticsObjects(IRI type, int limit, int offset) {
        return this.getRepository().listAllLogisticsObjects(type, limit, offset);
    }
}
