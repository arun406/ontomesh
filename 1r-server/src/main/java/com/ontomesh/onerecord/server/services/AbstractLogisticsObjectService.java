package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import com.ontomesh.onerecord.server.exception.RevisionMissingException;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.iata.onerecord.api.API;

import java.net.URI;
import java.util.Optional;

@Slf4j
public abstract class AbstractLogisticsObjectService {

    public final LogisticsObjectRepository repository;
    public final IdProvider idProvider;

    protected AbstractLogisticsObjectService(LogisticsObjectRepository repository,
                                             IdProvider idProvider) {
        this.repository = repository;
        this.idProvider = idProvider;
    }

    /**
     * returns the snapshot version.
     *
     * @param logisticsObject logistics object.
     * @return logistics object revision
     */
    public int getRevision(LogisticsObject logisticsObject) {
        Model model = logisticsObject.model();
        IRI iri = logisticsObject.iri();
        Optional<Literal> optionalLiteral = Models.objectLiteral(model.filter(iri, API.hasRevision, null));
        return optionalLiteral.orElseThrow(() -> new RevisionMissingException(iri)).intValue();
    }

    /**
     * Latest logistics object revision from the repository by iri.
     *
     * @param iri logistics object id.
     * @return revision.
     */
    public Integer getRevision(IRI iri) {
        RepositoryResult<Statement> statements = this.repository
                .getStatements(iri, API.hasRevision, null);
        if (statements == null) {
            return null;
        }
        Statement next = statements.next();
        if (!next.getObject().isLiteral()) {
            return null;
        }
        return Integer.parseInt(next.getObject().stringValue());
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
        return repository.findLoById(iri)
                .orElseThrow(() -> new LogisticsObjectNotFoundException(iri, "logistics object not found."));
    }
}
