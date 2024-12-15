package io.ontomesh.onerecord.server.services.logisticsobjects;

import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
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
