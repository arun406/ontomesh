package io.ontomesh.onerecord.server.repository;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
                       FILTER (!BOUND(?STATUS) || ?status = ?STATUS)
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

    public Model findEventModelById(IRI id) {
        String query = String.format(
                "   PREFIX cargo: <https://onerecord.iata.org/ns/cargo#>" +
                        "   PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "   CONSTRUCT WHERE { " +
                        "      <%s> ?p ?o ; " +
                        "      cargo:eventFor ?lo ; " +
                        "      cargo:recordingOrganization ?recordingOrganization ; " +
                        "      cargo:eventTimeType ?eventTimeType ; " +
                        "      cargo:eventCode ?code . " +
                        "      ?lo rdf:type ?loType . " +
                        "      ?recordingOrganization rdf:type ?recordingOrganizationType . " +
                        "      ?eventTimeType rdf:type cargo:EventTimeType . " +

                        "      ?code rdf:type ?codeType . " +
                        "      ?code cargo:code ?codeValue . " +
                        "      ?code cargo:codeListName ?codeListName . " +
                        " } "
                , id);
        GraphQueryResult queryResult = getConnection().prepareGraphQuery(query).evaluate();
        return QueryResults.asModel(queryResult);
    }

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

    public Optional<LogisticsEvent> findEventsByLogisticsObjectId(IRI iri, Predicate<Statement> continueRecursionIf) {
        return findByIRI(iri, continueRecursionIf)
                .map(model -> new LogisticsEvent(iri, model));
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
        query = queryWithStatus.formatted(type.toString(), limit, offset);

        log.trace("query: {}", query);

        TupleQuery tupleQuery = getConnection().prepareTupleQuery(query);
        tupleQuery.setBinding("STATUS", status);
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
        log.trace("Query : {}", query);
        Update operation = getConnection().prepareUpdate(QueryLanguage.SPARQL, query);
        operation.setBinding("subject", logisticsObjectId);
        operation.setBinding("currentRevision", currentRevision);
        operation.execute();
        Statement next = getConnection().getStatements(logisticsObjectId, API.hasRevision, null).next();
        return (Literal) next.getObject();
    }

    /**
     * @param loType
     * @param limit
     * @param offset
     * @return
     */
    public List<IRI> listAllLogisticsObjects(IRI loType, int limit, int offset) {

        final String LIST_LOGISTICS_OBJECT_BY_TYPE = String.format("""
                PREFIX cargo: <https://onerecord.iata.org/ns/cargo#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                SELECT DISTINCT ?id
                WHERE {
                    ?id rdf:type ?class .
                    ?class rdf:subClassOf* ?mid .
                    ?mid rdfs:subClassOf* cargo:LogisticsObject .
                    FILTER (!BOUND(?LO_TYPE) || ?class = ?LO_TYPE) 
                }
                ORDER BY ?id
                LIMIT %d OFFSET %d
                """, limit, offset);

        TupleQuery tupleQuery = getConnection().prepareTupleQuery(LIST_LOGISTICS_OBJECT_BY_TYPE);
        tupleQuery.setIncludeInferred(false);
        if (loType != null)
            tupleQuery.setBinding("LO_TYPE", loType);
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            List<BindingSet> list = QueryResults.asList(result);
            if (list == null) {
                return null;
            }
            return list.stream().map(bindings -> (IRI) bindings.getValue("id"))
                    .collect(Collectors.toList());
        }
    }
}
