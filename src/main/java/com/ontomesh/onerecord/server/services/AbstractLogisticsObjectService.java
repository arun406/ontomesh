package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.RevisionMissingException;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.iata.onerecord.api.API;

import java.util.Optional;

public abstract class AbstractLogisticsObjectService {
    public final LogisticsObjectRepository repository;
    public final IdProvider idProvider;

    protected AbstractLogisticsObjectService(LogisticsObjectRepository repository, IdProvider idProvider) {
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
        Optional<Literal> optionalLiteral = Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), API.hasRevision, null));
        return optionalLiteral.orElseThrow(() -> new RevisionMissingException(logisticsObject.iri())).intValue();
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
}
