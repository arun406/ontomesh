package io.ontomesh.onerecord.server.repository;

import io.ontomesh.onerecord.server.api.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

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
                    IRI type = RDFUtil.getType(model, iri);
                    return new LogisticsObject(iri, type, model);
                });
    }

    public Optional<LogisticsObject> findLoById(IRI iri, Predicate<IRI> continueRecursionIf) {
        return findByIRI(iri, continueRecursionIf)
                .map(model -> {
                    IRI type = RDFUtil.getType(model, iri);
                    return new LogisticsObject(iri, type, model);
                });
    }
}
