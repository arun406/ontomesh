package io.ontomesh.events.processor.repository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Base repository class.
 */
@Getter
@Slf4j
public abstract class BaseRepository {

    private final RepositoryConnection connection;

    protected BaseRepository(RepositoryConnection connection) {
        this.connection = connection;
    }
}
