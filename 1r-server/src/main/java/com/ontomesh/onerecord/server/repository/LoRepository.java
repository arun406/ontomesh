package com.ontomesh.onerecord.server.repository;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class LoRepository extends BaseRepository {

    public Optional<LogisticsObject> findLoById(IRI iri) {
        return findByIRI(iri).map(model -> new LogisticsObject(iri, model));
    }
}
