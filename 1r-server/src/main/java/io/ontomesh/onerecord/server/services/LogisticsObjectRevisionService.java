package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.RevisionMissingException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogisticsObjectRevisionService {
    public final LogisticsObjectRepository repository;

    public LogisticsObjectRevisionService(LogisticsObjectRepository repository) {
        this.repository = repository;
    }

    public void incrementLogisticsObjectRevision(LogisticsObject logisticsObject) {

        IRI iri = logisticsObject.iri();
        Integer currentRevisionNumber = getRevision(iri);
        if (currentRevisionNumber == null) {
            throw new RevisionMissingException("Invalid Logistics Object. Revision is not available.", iri);
        }

        Literal currentRevision = Values.literal(currentRevisionNumber);
        Literal newRevision = Values.literal(currentRevisionNumber + 1);

        saveOrUpdate(logisticsObject.iri(), currentRevision, newRevision);
        logisticsObject.updateRevision(newRevision);
    }

    private void saveOrUpdate(IRI iri, Literal currentRevision, Literal newRevision) {
        this.repository.delete(iri, API.hasRevision, currentRevision);
        this.repository.add(iri, API.hasRevision, newRevision);
//        // update the local object.
//        logisticsObject.model().remove(iri, API.hasRevision, null);
//        logisticsObject.model().add(iri, API.hasRevision, newRevision);
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
