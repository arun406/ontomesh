package io.ontomesh.onerecord.server.repository;

import io.ontomesh.core.rdf.repository.BaseRepository;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsEventNotFoundException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectQueryException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectVersionException;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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

    public LogisticsObjectRepository(Repository repository) {
        super(repository);
    }

    /**
     * Get a connection to the underlying repository.
     * 
     * @return RepositoryConnection
     */
    public RepositoryConnection getConnection() {
        return getRepository().getConnection();
    }

    private static final String QUERY_WITH_STATUS = """
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

    private static final String QUERY_WITHOUT_STATUS = """
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
     * Find event model by ID using SPARQL CONSTRUCT query.
     * 
     * @param id the IRI of the event
     * @return Model containing the event data
     * @throws LogisticsObjectQueryException if the query fails
     */
    public Model findEventModelById(IRI id) {
        String query = String.format("""
                PREFIX cargo: <https://onerecord.iata.org/ns/cargo#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                CONSTRUCT WHERE {
                   <%s> ?p ?o ;
                   cargo:eventFor ?lo ;
                   cargo:recordingOrganization ?recordingOrganization ;
                   cargo:eventTimeType ?eventTimeType ;
                   cargo:eventCode ?code .
                   ?lo rdf:type ?loType .
                   ?recordingOrganization rdf:type ?recordingOrganizationType .
                   ?eventTimeType rdf:type cargo:EventTimeType .
                   ?code rdf:type ?codeType .
                   ?code cargo:code ?codeValue .
                   ?code cargo:codeListName ?codeListName .
                }
                """, id.stringValue());

        try (var conn = getRepository().getConnection();
             GraphQueryResult queryResult = conn.prepareGraphQuery(query).evaluate()) {
            return QueryResults.asModel(queryResult);
        } catch (Exception e) {
            log.error("Failed to find event model by ID: {}", id, e);
            throw new LogisticsObjectQueryException("Could not find event model by ID: " + id, e);
        }
    }

    /**
     * Find Logistics Object by IRI using the new SPARQL method.
     * 
     * @param iri the IRI of the Logistics Object
     * @return Optional containing the Logistics Object
     * @throws LogisticsObjectQueryException if the query fails
     */
    public Optional<LogisticsObject> findLoById(IRI iri) {
        try {
            return findByIRIWithSparql(iri)
                    .map(model -> {
                        IRI type = RDFUtil.getType(model, iri);
                        return new LogisticsObject(iri, type, model);
                    });
        } catch (Exception e) {
            log.error("Failed to find Logistics Object by ID: {}", iri, e);
            throw new LogisticsObjectQueryException("Could not find Logistics Object by ID: " + iri, e);
        }
    }

    /**
     * Find Logistics Object by IRI with custom recursion predicate.
     * 
     * @param iri the IRI of the Logistics Object
     * @param continueRecursionIf predicate to determine recursion
     * @return Optional containing the Logistics Object
     * @throws LogisticsObjectQueryException if the query fails
     */
    public Optional<LogisticsObject> findLoById(IRI iri, Predicate<Statement> continueRecursionIf) {
        try {
            return findByIRIWithSparql(iri)
                    .map(model -> {
                        IRI type = RDFUtil.getType(model, iri);
                        return new LogisticsObject(iri, type, model);
                    });
        } catch (Exception e) {
            log.error("Failed to find Logistics Object by ID with recursion: {}", iri, e);
            throw new LogisticsObjectQueryException("Could not find Logistics Object by ID: " + iri, e);
        }
    }

    /**
     * Find Logistics Events by Logistics Object ID.
     * 
     * @param iri the IRI of the Logistics Object
     * @param continueRecursionIf predicate to determine recursion
     * @return Optional containing the Logistics Event
     * @throws LogisticsEventNotFoundException if the event is not found
     * @throws LogisticsObjectQueryException if the query fails
     */
    public Optional<LogisticsEvent> findEventsByLogisticsObjectId(IRI iri, Predicate<Statement> continueRecursionIf) {
        try {
            return findByIRIWithSparql(iri)
                    .map(model -> new LogisticsEvent(iri, model));
        } catch (Exception e) {
            log.error("Failed to find Logistics Events by Logistics Object ID: {}", iri, e);
            throw new LogisticsEventNotFoundException("Could not find Logistics Events for Logistics Object ID: " + iri, e);
        }
    }

    /**
     * List Logistics Objects with status filtering.
     *
     * @param type   Logistics Object Type
     * @param limit  size
     * @param offset offset
     * @param status status (can be null)
     * @return List of ActionRequest
     * @throws LogisticsObjectQueryException if the query fails
     */
    public List<ActionRequest> listLogisticsObject(IRI type, int limit, int offset, IRI status) {
        String query = status != null 
            ? QUERY_WITH_STATUS.formatted(type.stringValue(), limit, offset)
            : QUERY_WITHOUT_STATUS.formatted(type.stringValue(), limit, offset);

        log.trace("Executing query: {}", query);

        try (var conn = getRepository().getConnection()) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(query);
            if (status != null) {
                tupleQuery.setBinding("STATUS", status);
            }
            
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                List<BindingSet> list = QueryResults.asList(result);
                if (list == null) {
                    return new ArrayList<>();
                }
                
                return list.stream()
                        .map(bindings -> {
                            IRI id = (IRI) bindings.getValue("id");
                            IRI statusValue = (IRI) bindings.getValue("status");
                            Literal requestedAt = (Literal) bindings.getValue("requestedAt");
                            
                            LocalDateTime dateTime = requestedAt != null 
                                ? LocalDateTime.from(requestedAt.temporalAccessorValue())
                                : null;
                                
                            return new ActionRequest(id, statusValue, dateTime);
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to list logistics objects for type: {}", type, e);
            throw new LogisticsObjectQueryException("Could not list logistics objects for type: " + type, e);
        }
    }

    /**
     * Increment the current revision of a Logistics Object.
     *
     * @param logisticsObjectId Logistics Object Id
     * @param currentRevision   Current revision of Logistics Object.
     * @return the new revision literal
     * @throws LogisticsObjectVersionException if the version increment fails
     * @throws LogisticsObjectNotFoundException if the Logistics Object is not found
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
        
        log.trace("Executing update query: {}", query);
        
        try (var conn = getRepository().getConnection()) {
            Update operation = conn.prepareUpdate(QueryLanguage.SPARQL, query);
            operation.setBinding("subject", logisticsObjectId);
            operation.setBinding("currentRevision", currentRevision);
            operation.execute();
            
            // Get the updated revision
            try (var statements = conn.getStatements(logisticsObjectId, API.hasRevision, null)) {
                if (statements.hasNext()) {
                    Statement next = statements.next();
                    return (Literal) next.getObject();
                } else {
                    throw new LogisticsObjectNotFoundException("Failed to retrieve updated revision for Logistics Object: " + logisticsObjectId);
                }
            }
        } catch (LogisticsObjectNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to increment logistics object version for: {}", logisticsObjectId, e);
            throw new LogisticsObjectVersionException("Could not increment logistics object version for: " + logisticsObjectId, e);
        }
    }

    /**
     * List all Logistics Objects by type.
     *
     * @param loType the type of Logistics Object (can be null for all types)
     * @param limit  size limit
     * @param offset offset
     * @return List of IRI identifiers
     * @throws LogisticsObjectQueryException if the query fails
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

        try (var conn = getRepository().getConnection()) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(LIST_LOGISTICS_OBJECT_BY_TYPE);
            tupleQuery.setIncludeInferred(false);
            if (loType != null) {
                tupleQuery.setBinding("LO_TYPE", loType);
            }
            
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                List<BindingSet> list = QueryResults.asList(result);
                if (list == null) {
                    return new ArrayList<>();
                }
                
                return list.stream()
                        .map(bindings -> (IRI) bindings.getValue("id"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to list all logistics objects for type: {}", loType, e);
            throw new LogisticsObjectQueryException("Could not list all logistics objects for type: " + loType, e);
        }
    }
}
