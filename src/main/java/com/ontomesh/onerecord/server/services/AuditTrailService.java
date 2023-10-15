package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.SubjectNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.Date;
import java.time.Instant;

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


    /**
     * Saves and Update the Audit trail =
     *
     * @param changeRequestIRI
     * @param logisticsObjectIRI
     * @return
     */
    @Transactional
    public IRI saveOrUpdate(IRI changeRequestIRI, IRI logisticsObjectIRI) {

        IRI auditTrailId = getAuditIri(URI.create(logisticsObjectIRI.toString()));

        LogisticsObject auditTrail = this.repository.findLoById(auditTrailId, iri -> false)
                .orElseGet(() -> this.create(auditTrailId, changeRequestIRI));
        auditTrail.model().add(auditTrail.iri(), API.hasChangeRequest, changeRequestIRI);
        save(auditTrail);
        updateLatestRevision(auditTrailId);
        return auditTrailId;
    }

    private void save(LogisticsObject auditTrail) {
        this.repository.persist(auditTrail.iri(), auditTrail.model());
    }

    private LogisticsObject create(IRI auditTrailId, IRI changeRequestIRI) {
        Model auditTrail = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(auditTrailId)
                .add(RDF.TYPE, API.AuditTrail)
                .add(API.hasChangeRequest, changeRequestIRI)
                .add(API.hasLatestRevision, Values.literal(0)).build();
        return new LogisticsObject(auditTrailId, API.AuditTrail, auditTrail);
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

    private boolean recursive(IRI iri) {
        return false;
    }

    /**
     * Updates the Status
     *
     * @param changeRequestId
     */
    @Transactional
    public void updateChangeRequestStatus(IRI changeRequestId) {
        RepositoryResult<Statement> statements = this.repository.getStatements(changeRequestId, API.hasRequestStatus, REQUEST_PENDING);
        if (statements == null) {
            return;
        }
        Statement next = statements.next();
        if (next.getObject().isIRI()) {
            // First Remove the existing statement.
            this.repository.delete(changeRequestId, API.hasRequestStatus, REQUEST_PENDING);
            //Add New statement
            this.repository.add(changeRequestId, API.hasRequestStatus, REQUEST_ACCEPTED);
        }
    }

    /**
     * @param auditTrailId
     */
    private void updateLatestRevision(IRI auditTrailId) {
        RepositoryResult<Statement> statements = this.repository.getStatements(auditTrailId, API.hasLatestRevision, null);
        if (statements == null) {
            return;
        }
        Statement next = statements.next();
        if (next.getObject().isLiteral()) {
            int revision = Integer.parseInt(next.getObject().stringValue());
            // First Remove the existing statement.
            this.repository.delete(auditTrailId, API.hasLatestRevision, Values.literal(revision));
            //Add New statement
            this.repository.add(auditTrailId, API.hasLatestRevision, Values.literal(revision + 1));
        }
    }

    /**
     * Saves the change request
     *
     * @param change
     * @return
     */
    public IRI saveChangeRequest(LogisticsObject change) {

        IRI changeRequestId = idProvider.createUniqueLoUri().iri();
        log.debug("change request id : {}", changeRequestId);

        this.repository.persist(change.iri(), change.model());

        Model changeRequest = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(changeRequestId)
                .add(RDF.TYPE, API.ChangeRequest)
                .add(API.hasChange, change.iri())
                .add(API.isRequestedAt, Date.from(Instant.now()))
                .add(API.isRequestedBy, Values.literal("Arun Kumar"))
                .add(API.hasRequestStatus, REQUEST_PENDING)
                .build();

        this.repository.persist(changeRequestId, changeRequest);
        return changeRequestId;
    }

    /**
     * returns the complete Audit Trail of an Logistics Object
     *
     * @param auditTrailId
     * @return
     */
    public LogisticsObject getAuditTrail(URI auditTrailId) {
        return this.repository.findLoById(Values.iri(auditTrailId.toString()), this::isValidIri)
                .orElseThrow(SubjectNotFoundException::new);
    }
}
