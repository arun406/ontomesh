package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import com.ontomesh.onerecord.server.util.RDFUtil;
import com.ontomesh.onerecord.server.util.URIUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.sql.Date;
import java.time.Instant;
import java.util.function.Predicate;

import static org.iata.onerecord.api.API.REQUEST_ACCEPTED;
import static org.iata.onerecord.api.API.REQUEST_PENDING;

@Slf4j
@Service
public class AuditTrailService {

    private final IdProvider idProvider;
    private final LogisticsObjectRepository repository;

    public AuditTrailService(IdProvider idProvider, LogisticsObjectRepository repository) {
        this.idProvider = idProvider;
        this.repository = repository;
    }


    public IRI createAuditTrail(LogisticsObject logisticsObject, URI loId) {
        IRI changeId = logisticsObject.iri();
        log.debug("change changeId :{}, lo id :{}", changeId, loId);

        IRI changeRequestId = idProvider.createInternalIri().iri();
        log.debug("change request id : {}", changeRequestId);

        Model changeRequest = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(changeRequestId)
                .add(RDF.TYPE, API.ChangeRequest)
                .add(API.hasChange, logisticsObject.iri())
//                .add(API.hasError, null)
                .add(API.isRequestedAt, Date.from(Instant.now()))
                .add(API.isRequestedBy, Values.literal("Arun Kumar"))
                .add(API.hasRequestStatus, REQUEST_PENDING)
                .build();

        boolean b1 = changeRequest.addAll(logisticsObject.model());

        IRI auditTrailId = getAuditIri(loId);

        Model auditTrail = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(auditTrailId)
                .add(RDF.TYPE, API.AuditTrail)
                .add(API.hasChangeRequest, changeRequestId)
                .add(API.hasLatestRevision, Values.literal(1)).build();

        boolean b = auditTrail.addAll(changeRequest);

        this.repository.persist(auditTrailId, auditTrail);

        return auditTrailId;
    }

    /**
     * @param uri
     * @return
     */
    private IRI getAuditIri(URI uri) {
        URI auditTrailUri = URIUtil.addPath(uri, "audit-trail");
        return RDFUtil.createIRI(auditTrailUri);
    }


    /**
     * @param iri
     * @return
     */
    private boolean isValidIri(IRI iri) {
        return (OntoMeshId.isInternalIri(iri)) || OntoMeshId.isOneRecordIri(iri)
                || iri.getNamespace().equals(idProvider.getLogisticsObjectBaseIRI().stringValue());
    }

    /**
     * Updates the Status
     *
     * @param changeRequestId
     */
    public void updateChangeRequestStatus(IRI changeRequestId) {
        RepositoryResult<Statement> statements = this.repository.getStatements(changeRequestId, API.hasRequestStatus, REQUEST_PENDING);
        if (statements == null) {
            return;
        }
        Statement next = statements.next();
        if (next.getObject().isLiteral()) {
            String status = next.getObject().stringValue();
            // First Remove the existing statement.
            this.repository.delete(changeRequestId, API.hasRequestStatus, Values.literal(status));
            //Add New statement
            this.repository.add(changeRequestId, API.hasRequestStatus, Values.literal(REQUEST_ACCEPTED));
        }
    }

    /**
     * Saves the change request
     *
     * @param logisticsObject
     * @return
     */
    public void saveChangeRequest(LogisticsObject logisticsObject) {

        IRI changeId = logisticsObject.iri();
        log.debug("change changeId :{}", changeId);

        IRI changeRequestId = idProvider.createInternalIri().iri();
        log.debug("change request id : {}", changeRequestId);

        Model changeRequest = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(changeRequestId)
                .add(RDF.TYPE, API.ChangeRequest)
                .add(API.hasChange, logisticsObject.iri())
                .add(API.isRequestedAt, Date.from(Instant.now()))
                .add(API.isRequestedBy, Values.literal("Arun Kumar"))
                .add(API.hasRequestStatus, REQUEST_PENDING)
                .build();

        boolean result = changeRequest.addAll(logisticsObject.model());

        this.repository.persist(changeRequestId, changeRequest);
    }

    /**
     * @param changeRequestId change request id
     * @return logistics Object of type api:ChangeRequest
     */
    public LogisticsObject getChangeRequestById(IRI changeRequestId) {
        log.debug("Finding Change request by Id {}", changeRequestId);
        Predicate<IRI> isValidIri = this::isValidIri;
        return this.repository.findLoById(changeRequestId, isValidIri).orElseThrow();
    }
}
