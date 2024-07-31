package io.ontomesh.events.processor.repository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

@Getter
@Slf4j
public abstract class BaseRepository {

    private final RepositoryConnection connection;

    protected BaseRepository(RepositoryConnection connection) {
        this.connection = connection;
    }

    /**
     * @param resource
     * @param predicate
     * @param object
     * @return
     */
    public RepositoryResult<Statement> getStatements(Resource resource, IRI predicate, Value object) {
        return connection.getStatements(resource, predicate, object);
    }
}
