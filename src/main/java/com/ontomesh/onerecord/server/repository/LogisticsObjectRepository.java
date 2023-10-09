package com.ontomesh.onerecord.server.repository;

import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

import static com.ontomesh.onerecord.server.util.RDFUtil.getType;

@Component
@Slf4j
public class LogisticsObjectRepository extends BaseRepository {

    /**
     * @param iri
     * @return
     */
    public Optional<LogisticsObject> findLoById(IRI iri) {
        return findByIRI(iri, OntoMeshId::isInternalIri)
                .map(model -> {
                    IRI type = getType(model, iri);
                    return new LogisticsObject(iri, type, model);
                });
    }

    public Optional<LogisticsObject> findLoById(IRI iri, Predicate<IRI> continueRecursionIf) {
        return findByIRI(iri, continueRecursionIf)
                .map(model -> {
                    IRI type = getType(model, iri);
                    return new LogisticsObject(iri, type, model);
                });
    }
}
