package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.InvalidChangeOperationException;
import com.ontomesh.onerecord.server.exception.InvalidRevisionException;
import com.ontomesh.onerecord.server.exception.RevisionMissingException;
import com.ontomesh.onerecord.server.exception.SubjectNotFoundException;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import com.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ontomesh.onerecord.server.util.RDFUtil.getObject;
import static com.ontomesh.onerecord.server.util.RDFUtil.getObjectLiteral;

@Slf4j
@Service
public class LogisticsObjectService {

    public static final String DELETE = "https://onerecord.iata.org/ns/api#DELETE";
    public static final String ADD = "https://onerecord.iata.org/ns/api#ADD";
    public final LogisticsObjectRepository repository;

    @Autowired
    private final IdProvider idProvider;

    public LogisticsObjectService(LogisticsObjectRepository repository, IdProvider idProvider) {
        this.repository = repository;
        this.idProvider = idProvider;
    }

    @Transactional
    public void saveLogisticsObject(LogisticsObject logisticObject) {
        this.repository.persist(logisticObject.iri(), logisticObject.model());
    }

    @Transactional
    public boolean isExists(IRI iri) {
        Optional<LogisticsObject> optionalModel = this.repository.findLoById(iri);
        return optionalModel.isPresent();
    }

    @Transactional
    public LogisticsObject findLogisticsObject(URI uri) {
        IRI iri = Values.iri(uri.toString());
        log.info("IRI To Search: {}", iri.toString());
        return repository.findLoById(iri)
                .orElseThrow(SubjectNotFoundException::new);
    }

    @Transactional
    public LogisticsObject findLogisticsObject(IRI iri) {
        return repository.findLoById(iri)
                .orElseThrow(SubjectNotFoundException::new);
    }


    @Transactional
    public Set<IRI> updateLogisticObject(OntoMeshId loId, LogisticsObject change) {
        log.debug("Updating Logistics Object [{}]", loId.iri());

        URI loIdUri = loId.uri();
        Model changeModel = change.model();

        LogisticsObject logisticsObject = findLogisticsObject(loIdUri);
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
        return changedProperties;
    }

    private void updateLogisticObjectRevision(LogisticsObject logisticsObject) {
        IRI loId = logisticsObject.iri();
        RepositoryResult<Statement> statements = this.repository.getStatements(loId, API.hasRevision, null);
        if (statements != null) {
            Statement next = statements.next();
            if (next.getObject().isLiteral()) {
                int revision = Integer.parseInt(next.getObject().stringValue());
                // First Remove the existing statement.
                this.repository.delete(loId, API.hasRevision, Values.literal(revision));
                //Add New statement
                this.repository.add(loId, API.hasRevision, Values.literal(revision + 1));
            }
        }
    }

    private void validateRevision(LogisticsObject change, LogisticsObject logisticsObject) {

        log.debug("logistics object iri :{}", logisticsObject.iri());

        int changeRevision = getRevision(change);
        int loRevision = getRevision(logisticsObject);
        if (loRevision != changeRevision) {
            throw new InvalidRevisionException(logisticsObject.iri());
        }
    }

    private int getRevision(LogisticsObject logisticsObject) {
        Optional<Literal> optionalLiteral = Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), API.hasRevision, null));
        return optionalLiteral.orElseThrow(() -> new RevisionMissingException(logisticsObject.iri())).intValue();
    }

    private void processAddOperations(LogisticsObject logisticsObject, Set<IRI> changedProperties, Set<IRI> operationObjects) {
        log.debug("Applying Add operations...");
        Model changeModel = logisticsObject.model();
        Map<String, IRI> bnodeToIri = getBnodeToIri(changeModel, operationObjects, ADD);

        for (IRI opSubject : operationObjects) {
            IRI patchOperation = getObject(opSubject, API.op, changeModel);
            if (!patchOperation.toString().equalsIgnoreCase(ADD)) {
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
            IRI subject = bnodeToIri.containsKey(subjectAsString) ? bnodeToIri.get(subjectAsString) : Values.iri(subjectAsString);

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
                IRI objectIri = bnodeToIri.containsKey(value)
                        ? bnodeToIri.get(value)
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
        Map<String, IRI> bnodeToIri = getBnodeToIri(changeModel, operationObjects, DELETE);

        for (IRI operationObject : operationObjects) {
            IRI patchOperation = getObject(operationObject, API.op, changeModel);
            if (!patchOperation.toString().equalsIgnoreCase(DELETE)) {
                continue;
            }

            IRI predicate = Values.iri(getObjectLiteral(operationObject, API.p, changeModel).stringValue());

            // get the predicate
            changedProperties.add(predicate);

            String subjectAsString = getObjectLiteral(operationObject, API.s, changeModel).stringValue();
//            IRI s = Values.iri(subjectAsString);
            // either the opSubject is an IRI or a blank node that is mapped to an internal IRI
            IRI subject = bnodeToIri.containsKey(subjectAsString) ? bnodeToIri.get(subjectAsString) : Values.iri(subjectAsString);
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

    private Map<String, IRI> getBnodeToIri(Model changeModel, Set<IRI> operationObjects, String operation) {
        Map<String, IRI> bnodeToIri = operationObjects.stream()
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
        return bnodeToIri;
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
}
