package io.ontomesh.onerecord.server.repository;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.*;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
@Slf4j
public class LogisticsObjectRepository extends BaseRepository {

    String queryWithStatus = """
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                prefix api: <https://onerecord.iata.org/ns/api#>
                SELECT DISTINCT ?id ?status ?requestedAt
                WHERE {
                    ?id rdf:type <%s> ;
                       api:hasRequestStatus ?status ;
                       api:isRequestedAt ?requestedAt .
                       FILTER (?status = <%s> ) .
                }
                ORDER BY (?requestedAt)
                LIMIT %d
                OFFSET %d
            """;

    String queryWithOutStatus = """
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                prefix api: <https://onerecord.iata.org/ns/api#>
                SELECT DISTINCT ?id ?status ?requestedAt
                WHERE {
                    ?id rdf:type <%s> ;
                       api:hasRequestStatus ?status ;
                       api:isRequestedAt ?requestedAt .
                }
                ORDER BY (?requestedAt)
                LIMIT %d
                OFFSET %d
            """;

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

    public Optional<LogisticsObject> findLoById(IRI iri, Predicate<Statement> continueRecursionIf) {
        return findByIRI(iri, continueRecursionIf)
                .map(model -> {
                    IRI type = RDFUtil.getType(model, iri);
                    return new LogisticsObject(iri, type, model);
                });
    }

    public List<IRI> logisticsObjectTypes() {
        List<IRI> loTypes = new ArrayList<>();
        String query = """
                     prefix owl: <http://www.w3.org/2002/07/owl#>
                     prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                     prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                     SELECT DISTINCT ?class
                     WHERE {
                       ?class rdf:type owl:Class .
                       ?class rdf:subClassOf* ?mid .
                       ?mid rdfs:subClassOf* <https://onerecord.iata.org/ns/cargo#LogisticsObject> .
                     }
                """;

        TupleQuery tupleQuery = getConnection().prepareTupleQuery(query);
        try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
            while (evaluate.hasNext()) {
                BindingSet next = evaluate.next();
                for (Binding binding : next) {
                    log.info("name: {}, value : {} ", binding.getName(), binding.getValue());
                    loTypes.add((IRI) binding.getValue());
                }
            }
        }
        return loTypes;
    }

    /**
     * list the Logistics Objects
     *
     * @param type   Logistics Object Type
     * @param limit  size
     * @param offset offset
     * @param status status
     * @return List of ActionRequest
     */
    public List<ActionRequest> listLogisticsObject(IRI type, int limit, int offset, IRI status) {
        String query;
        if (status != null) {
            query = queryWithStatus.formatted(type.toString(), status.toString(), limit, offset);
        } else {
            query = queryWithOutStatus.formatted(type.toString(), limit, offset);
        }

        log.info("query: {}", query);

        TupleQuery tupleQuery = getConnection().prepareTupleQuery(query);

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            List<BindingSet> list = QueryResults.asList(result);
            return
                    list.stream()
                            .map(bindings -> new ActionRequest((IRI) bindings.getValue("id"),
                                    (IRI) bindings.getValue("status"),
                                    LocalDateTime.from(((Literal) bindings.getValue("requestedAt")).temporalAccessorValue())))
                            .toList();
        }
    }

    /**
     * Increment the current revision of a Logistics Object.
     *
     * @param logisticsObjectId Logistics Object Id
     * @param currentRevision   Current revision of Logistics Object.
     */
    public Literal incrementLogisticsObjectVersion(IRI logisticsObjectId, Literal currentRevision) {
        String query = """
                        prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        prefix api: <https://onerecord.iata.org/ns/api#>
                        prefix cargo: <https://onerecord.iata.org/ns/cargo#>
                        DELETE { ?subject api:hasRevision ?current }
                        INSERT { ?subject api:hasRevision ?new }
                        WHERE {
                          	?subject rdf:type cargo:Piece ;
                            		  api:hasRevision ?current .
                            BIND((?current + 1 ) AS ?new )
                          	FILTER (?current = ?currentRevision)
                        }
                """;
        log.info("Query : {}", query);
        Update operation = getConnection().prepareUpdate(QueryLanguage.SPARQL, query);
        operation.setBinding("subject", logisticsObjectId);
        operation.setBinding("currentRevision", currentRevision);
        operation.execute();
        Statement next = getConnection().getStatements(logisticsObjectId, API.hasRevision, null).next();
        return (Literal) next.getObject();
    }
}
