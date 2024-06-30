package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsObjectException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.net.URI;
import java.util.function.Predicate;

@Getter
@Slf4j
public abstract class AbstractLogisticsObjectService {

    private final LogisticsObjectRepository repository;
    private final IdProvider idProvider;

    protected AbstractLogisticsObjectService(LogisticsObjectRepository repository,
                                             IdProvider idProvider) {
        this.repository = repository;
        this.idProvider = idProvider;
    }

    public IRI getTenant() {
        return this.idProvider.tenant();
    }

    protected int isTypePredicate(IRI iri1) {
        return iri1.toString().compareTo(RDF.TYPE.toString());
    }

    /**
     * Returns the Logistic object with URI.
     *
     * @param uri logistics object uri.
     * @return logistics object.
     */
    public LogisticsObject findLogisticsObject(URI uri) {
        IRI iri = Values.iri(uri.toString());
        return findLogisticsObject(iri);
    }

    /**
     * Returns the Logistic object with IRI.
     *
     * @param iri logistics object iri.
     * @return logistics object.
     */
    public LogisticsObject findLogisticsObject(IRI iri) {
        log.info("Searching for Logistics Object of IRI {}", iri.toString());
        Predicate<Statement> predicate = statement -> !(statement.getPredicate().equals(OntoMesh.hasLogisticsEvents));
        return repository.findLoById(iri, predicate)
                .orElseThrow(() -> new LogisticsObjectNotFoundException(iri, "logistics object not found."));
    }

    /**
     * Returns the Logistics Object Type.
     *
     * @param loId Logistics Object IRI.
     * @return Logistics Object Type
     */
    public IRI getType(IRI loId) {
        return Models.objectIRI(getRepository().getStatements(loId, RDF.TYPE, null))
                .orElseThrow(() -> new RuntimeException("Unable to get Logistics Object Type."));
    }

    protected void saveLogisticsObject(LogisticsObject logisticsObject) {
        getRepository().persist(logisticsObject.model());
    }

    /**
     * Validates Logistics Object
     *
     * @param logisticObject logistics Object
     * @throws LogisticsObjectException exception.
     */
    protected void validate(LogisticsObject logisticObject) throws LogisticsObjectException {
        boolean isValid = true;

        if (!isValid) {
            throw new LogisticsObjectException("Validation exception.");
        }
    }

//    /**
//     * @param logisticsObject       logistics object.
//     * @param notificationEventType event type.
//     */
//    protected void addEventToLogisticsObject(LogisticsObject logisticsObject, NotificationEventType notificationEventType) {
//        logisticsEventService.addEventToLogisticsObject(logisticsObject.iri(), notificationEventType);
//    }

}
