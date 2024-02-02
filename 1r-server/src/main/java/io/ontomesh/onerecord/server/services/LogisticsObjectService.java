package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.InvalidChangeOperationException;
import io.ontomesh.onerecord.server.exception.InvalidRevisionException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectException;
import io.ontomesh.onerecord.server.exception.RevisionMissingException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.ontomesh.onerecord.server.util.RDFUtil.getObject;
import static io.ontomesh.onerecord.server.util.RDFUtil.getObjectLiteral;

@Slf4j
@Service
public class LogisticsObjectService extends AbstractLogisticsObjectService {

    private final LogisticsEventService logisticsEventService;
    private final SnapshotService snapshotService;

    public LogisticsObjectService(LogisticsObjectRepository repository, IdProvider idProvider, LogisticsEventService logisticsEventService, SnapshotService snapshotService) {
        super(repository, idProvider);
        this.logisticsEventService = logisticsEventService;
        this.snapshotService = snapshotService;
    }

    public static NotificationEventType getNotificationEventType(IRI status) {
        if (status.equals(API.REQUEST_ACCEPTED)) {
            return NotificationEventType.CHANGE_REQUEST_ACCEPTED;
        } else if (status.equals(API.REQUEST_REJECTED)) {
            return NotificationEventType.CHANGE_REQUEST_REJECTED;
        } else if (status.equals(API.REQUEST_REVOKED)) {
            return NotificationEventType.CHANGE_REQUEST_REVOKED;
        } else if (status.equals(API.REQUEST_FAILED)) {
            return NotificationEventType.CHANGE_REQUEST_FAILED;
        }
        return NotificationEventType.CHANGE_REQUEST_PENDING;
    }

    /**
     * @param logisticsObject       logistics object.
     * @param notificationEventType event type.
     */
    private void addEventToLogisticsObject(LogisticsObject logisticsObject, NotificationEventType notificationEventType) {
        logisticsEventService.addEventToLogisticsObject(logisticsObject.iri(),
                notificationEventType);
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
                        (iri, iri2) -> iri
                ));
    }

    private void postProcess() {
        log.info("invoked post process.");
    }

    private void preProcess() {
        log.info("invoked pre process.");
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
                repository.add(objectIri, RDF.TYPE, Values.iri(datatype));
                //  - set the object to be the IRI of the "embedded object"
                object = objectIri;
            }
            repository.add(subject, predicate, object);
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
            int deletedTriples = repository.delete(subject, predicate, object);

            // if not exactly one triple is deleted throw an exception, this cancels the transaction and
            // nothing is changed
            if (deletedTriples != 1) {
                throw new RuntimeException("Number of affected triples should be 1 but was " + deletedTriples);
            }
        }
    }

    private IRI resolveAddOperationIri(String value) {
        IRI iri;
        if (value.startsWith("_:")) {
            iri = idProvider.createInternalIri().iri();
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
    private void updateLogisticObjectRevision(LogisticsObject logisticsObject) {

        Model model = logisticsObject.model();
        IRI iri = logisticsObject.iri();
        Integer revision = getRevision(iri);
        if (revision == null) {
            return;
        }
        // First Remove the existing statement.
        this.repository.delete(iri, API.hasRevision, Values.literal(revision));
        //Add New statement
        Literal newRevision = Values.literal(revision + 1);
        this.repository.add(iri, API.hasRevision, newRevision);
        // update the local object.
        model.remove(iri, API.hasRevision, null);
        model.add(iri, API.hasRevision, newRevision);
    }

    private void validate(LogisticsObject logisticObject) throws LogisticsObjectException {
        boolean isValid = true;

        if (!isValid) {
            throw new LogisticsObjectException("Validation exception.");
        }
    }

    /**
     * validates the latest logistics object revision with the change request revision.
     *
     * @param change          change request.
     * @param logisticsObject logistics object.
     */
    private void validateRevision(LogisticsObject change, LogisticsObject logisticsObject) {

        log.debug("logistics object iri :{}", logisticsObject.iri());

        int changeRevision = getRevision(change);
        int loRevision = getRevision(logisticsObject);
        if (loRevision != changeRevision) {
            throw new InvalidRevisionException(logisticsObject.iri());
        }
    }

    @Transactional
    public void createLogisticsObject(LogisticsObject logisticsObject) {

        validate(logisticsObject);
        // pre process

        preProcess();
        this.repository.persist(logisticsObject.model());

        // create and publish event.
        addEventToLogisticsObject(logisticsObject, NotificationEventType.LOGISTICS_OBJECT_CREATED);

        //post process
        postProcess();

    }

    @Transactional
    public boolean isExists(IRI iri) {
        Optional<LogisticsObject> optionalModel = this.repository.findLoById(iri);
        return optionalModel.isPresent();
    }

    public void processChangeRequest(LogisticsObject change, LogisticsObject logisticsObject) {


        Set<IRI> changedProperties = processActionRequest(change, logisticsObject);

        // create and publish event.
        addEventToLogisticsObject(logisticsObject, NotificationEventType.LOGISTICS_OBJECT_UPDATED);

        LogisticsObject snapshot = snapshotService.createSnapshot(logisticsObject);
        log.info("Snapshot with {} is created successfully.", snapshot.iri());


    }

    @Transactional
    public Set<IRI> processActionRequest(LogisticsObject change, LogisticsObject logisticsObject) {
        log.debug("Updating [{}]", logisticsObject);

        Model changeModel = change.model();

        validateRevision(change, logisticsObject);

        Set<IRI> changedProperties = new HashSet<>();
        Set<Resource> affectedSubjects = new HashSet<>();

        Set<IRI> operationObjects = RDFUtil.getObjects(change.iri(), API.hasOperation, changeModel);
        processDeleteOperations(change, changedProperties, affectedSubjects, operationObjects);
        processAddOperations(change, changedProperties, operationObjects);

        // check if any subject that was affected by a delete operation now only has the "type" triple left
        affectedSubjects.forEach(resource -> {
            Model model = repository.getStatements(resource, null, null).stream()
                    .collect(ModelCollector.toModel());
            if (model.size() == 1 && Models.predicate(model.filter(resource, RDF.TYPE, null)).isPresent()) {
                model.forEach(statement ->
                        repository.delete(
                                statement.getSubject(),
                                statement.getPredicate(),
                                statement.getObject()
                        )
                );
            }
        });
        // increase the Logistics Object revision after successful change
        updateLogisticObjectRevision(logisticsObject);
        Model model = logisticsObject.model();
        IRI iri = logisticsObject.iri();
        Model revisionModel = model.filter(iri, API.hasRevision, null);
        String revision = Models.objectString(revisionModel).orElse("unknown");
        log.debug("latest revision {}", revision);
        return changedProperties;
    }

    /**
     * returns the logistics object revision from the model.
     *
     * @param logisticsObject logistics object model.
     * @return revision.
     */
    public int getRevision(LogisticsObject logisticsObject) {
        Optional<Literal> optionalLiteral = Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), API.hasRevision, null));
        return optionalLiteral.orElseThrow(() -> new RevisionMissingException(logisticsObject.iri())).intValue();
    }
}
