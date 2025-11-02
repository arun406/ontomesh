package io.ontomesh.core.rdf.repository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Abstract base repository for RDF4J operations. Ensures thread safety, resource management, and error handling.
 */
@Getter
@Slf4j
public abstract class BaseRepository {

    private final Repository repository;

    public BaseRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * Get the underlying repository instance.
     * 
     * @return the repository instance
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Recursively traverse statements, following IRIs and adding their types.
     * Uses a Set to track visited statements to avoid infinite recursion.
     */
    private void traverse(RepositoryConnection connection, List<Statement> statements, Set<Statement> visitedStatements,
                          Predicate<Statement> continueTraversal) {
        for (Statement statement : statements) {
            if (visitedStatements.contains(statement)) {
                continue;
            }
            visitedStatements.add(statement);

            Value value = statement.getObject();
            if (!value.isIRI()) {
                continue;
            }

            // Get RDF.TYPE statements for this IRI
            try (RepositoryResult<Statement> typeResult = connection.getStatements((Resource) value, RDF.TYPE, null)) {
                while (typeResult.hasNext()) {
                    Statement typeStatement = typeResult.next();
                    visitedStatements.add(typeStatement);
                }
            } catch (Exception e) {
                log.error("Error traversing RDF.TYPE for value {}", value, e);
            }

            // Continue traversal if predicate allows
            if (continueTraversal.test(statement)) {
                try (RepositoryResult<Statement> bNodeResult = connection.getStatements((Resource) value, null, null)) {
                    List<Statement> bNodeStatements = new ArrayList<>();
                    while (bNodeResult.hasNext()) {
                        bNodeStatements.add(bNodeResult.next());
                    }
                    if (!bNodeStatements.isEmpty()) {
                        traverse(connection, bNodeStatements, visitedStatements, continueTraversal);
                    }
                } catch (Exception e) {
                    log.error("Error traversing bNode statements for value {}", value, e);
                }
            }
        }
    }

    /**
     * Find a Logistics Object by IRI using SPARQL CONSTRUCT query.
     * This is much more efficient than recursive traversal for complex objects.
     *
     * @param iri                   the IRI of the Logistics Object
     * @param includeRelatedObjects whether to include related objects (default: true)
     * @return Optional containing the found model, or empty if not found
     */
    public Optional<Model> findByIRIWithSparql(IRI iri, boolean includeRelatedObjects) {
        try (RepositoryConnection conn = repository.getConnection()) {
            String sparqlQuery = buildLogisticsObjectQuery(iri, includeRelatedObjects);
            log.debug("Executing SPARQL query for IRI: {}", iri);

            try (GraphQueryResult result = conn.prepareGraphQuery(sparqlQuery).evaluate()) {
                Model model = new DynamicModelFactory().createEmptyModel();
                while (result.hasNext()) {
                    model.add(result.next());
                }

                if (model.isEmpty()) {
                    log.debug("No statements found for IRI {}", iri);
                    return Optional.empty();
                }

                log.debug("Retrieved {} statements for IRI {} using SPARQL", model.size(), iri);
                return Optional.of(model);
            }
        } catch (Exception e) {
            log.error("Failed to find model by IRI using SPARQL: {}", iri, e);
            throw new RdfRepositoryException("Could not find model by IRI using SPARQL", e);
        }
    }

    /**
     * Find a Logistics Object by IRI using SPARQL CONSTRUCT query.
     * Includes related objects by default.
     *
     * @param iri the IRI of the Logistics Object
     * @return Optional containing the found model, or empty if not found
     */
    public Optional<Model> findByIRIWithSparql(IRI iri) {
        return findByIRIWithSparql(iri, true);
    }

    /**
     * Build SPARQL CONSTRUCT query for retrieving a Logistics Object and its related data.
     *
     * @param iri                   the IRI of the Logistics Object
     * @param includeRelatedObjects whether to include related objects
     * @return SPARQL query string
     */
    private String buildLogisticsObjectQuery(IRI iri, boolean includeRelatedObjects) {
        StringBuilder query = new StringBuilder();
        query.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        query.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
        query.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        query.append("PREFIX api: <https://onerecord.iata.org/ns/api#>\n");
        query.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>\n\n");

        query.append("CONSTRUCT {\n");
        query.append("  ?s ?p ?o .\n");

        if (includeRelatedObjects) {
            // Include related objects (objects that are IRIs)
            query.append("  ?o ?p2 ?o2 .\n");
            query.append("  ?o rdf:type ?type .\n");
        }

        query.append("}\n");
        query.append("WHERE {\n");
        query.append("  {\n");
        query.append("    ?s ?p ?o .\n");
        query.append("    FILTER(?s = <").append(iri.stringValue()).append(">)\n");
        query.append("  }\n");

        if (includeRelatedObjects) {
            // Include related objects that are IRIs (not literals or blank nodes)
            query.append("  UNION {\n");
            query.append("    ?s ?p ?o .\n");
            query.append("    FILTER(?s = <").append(iri.stringValue()).append(">)\n");
            query.append("    FILTER(isIRI(?o))\n");
            query.append("    ?o ?p2 ?o2 .\n");
            query.append("  }\n");
            query.append("  UNION {\n");
            query.append("    ?s ?p ?o .\n");
            query.append("    FILTER(?s = <").append(iri.stringValue()).append(">)\n");
            query.append("    FILTER(isIRI(?o))\n");
            query.append("    ?o rdf:type ?type .\n");
            query.append("  }\n");
        }

        query.append("}\n");

        return query.toString();
    }

    /**
     * Find a Logistics Object by IRI using SPARQL with custom query.
     * Useful for specific query requirements.
     *
     * @param iri         the IRI of the Logistics Object
     * @param customQuery the custom SPARQL CONSTRUCT query
     * @return Optional containing the found model, or empty if not found
     */
    public Optional<Model> findByIRIWithCustomSparql(IRI iri, String customQuery) {
        try (RepositoryConnection conn = repository.getConnection()) {
            log.debug("Executing custom SPARQL query for IRI: {}", iri);

            try (GraphQueryResult result = conn.prepareGraphQuery(customQuery).evaluate()) {
                Model model = new DynamicModelFactory().createEmptyModel();
                while (result.hasNext()) {
                    model.add(result.next());
                }

                if (model.isEmpty()) {
                    log.debug("No statements found for IRI {} with custom query", iri);
                    return Optional.empty();
                }

                log.debug("Retrieved {} statements for IRI {} using custom SPARQL", model.size(), iri);
                return Optional.of(model);
            }
        } catch (Exception e) {
            log.error("Failed to find model by IRI using custom SPARQL: {}", iri, e);
            throw new RdfRepositoryException("Could not find model by IRI using custom SPARQL", e);
        }
    }

    /**
     * Persist a model to the repository.
     */
    public void persist(Model model) {
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.add(model);
            log.debug("Persisted model with {} statements", model.size());
        } catch (Exception e) {
            log.error("Failed to persist model", e);
            throw new RdfRepositoryException("Could not persist model", e);
        }
    }

    /**
     * Find a model by IRI, traversing recursively as needed.
     *
     * @deprecated Use findByIRIWithSparql for better performance
     */
    @Deprecated
    public Optional<Model> findByIRI(IRI iri, Predicate<Statement> continueRecursionIf) {
        try (RepositoryConnection conn = repository.getConnection()) {
            List<Statement> baseStatements = new ArrayList<>();
            try (RepositoryResult<Statement> statements = conn.getStatements(iri, null, null)) {
                while (statements.hasNext()) {
                    baseStatements.add(statements.next());
                }
            }

            if (baseStatements.isEmpty()) {
                log.debug("No statements found for IRI {}", iri);
                return Optional.empty();
            }

            return findRecursively(conn, continueRecursionIf, baseStatements);
        } catch (Exception e) {
            log.error("Failed to find model by IRI {}", iri, e);
            throw new RdfRepositoryException("Could not find model by IRI", e);
        }
    }

    /**
     * Recursively build a model from statements.
     * Uses a single connection and efficient traversal to avoid connection exhaustion.
     *
     * @deprecated Use findByIRIWithSparql for better performance
     */
    @Deprecated
    public Optional<Model> findRecursively(RepositoryConnection conn, Predicate<Statement> continueRecursionIf, List<Statement> statements) {
        // Add initial statements to visited set
        Set<Statement> visitedStatements = new HashSet<>(statements);

        // Traverse recursively to find all related statements
        traverse(conn, statements, visitedStatements, continueRecursionIf);

        // Build model from all visited statements
        Model model = new DynamicModelFactory().createEmptyModel();
        model.addAll(visitedStatements);

        log.debug("Built model with {} statements (recursive)", model.size());
        return Optional.of(model);
    }

    /**
     * Delete statements matching the pattern.
     */
    public int delete(Resource subject, IRI predicate, Value object) {
        try (RepositoryConnection conn = repository.getConnection()) {
            // Count affected triples efficiently without loading all into memory
            int affectedTriples = 0;
            try (RepositoryResult<Statement> result = conn.getStatements(subject, predicate, object, true)) {
                while (result.hasNext()) {
                    result.next();
                    affectedTriples++;
                }
            }

            conn.remove(subject, predicate, object);
            log.debug("Deleted {} triples for subject {} predicate {} object {}", affectedTriples, subject, predicate, object);
            return affectedTriples;
        } catch (Exception e) {
            log.error("Failed to delete statements for subject {} predicate {} object {}", subject, predicate, object, e);
            throw new RdfRepositoryException("Could not delete statements", e);
        }
    }

    /**
     * Update a triple by deleting the old value and adding the new value.
     * Uses a single connection to avoid connection leaks.
     */
    public void update(Resource subject, IRI predicate, Value oldValue, Value newValue) {
        try (RepositoryConnection conn = repository.getConnection()) {
            log.debug("Updating statement for subject {} predicate {} from {} to {}", subject, predicate, oldValue, newValue);

            // Count and remove old statements
            int affectedTriples = 0;
            try (RepositoryResult<Statement> result = conn.getStatements(subject, predicate, oldValue, true)) {
                while (result.hasNext()) {
                    result.next();
                    affectedTriples++;
                }
            }
            conn.remove(subject, predicate, oldValue);

            // Add new statement
            conn.add(subject, predicate, newValue);

            log.debug("Updated {} triples for subject {} predicate {}", affectedTriples, subject, predicate);
        } catch (Exception e) {
            log.error("Failed to update statement for subject {} predicate {}", subject, predicate, e);
            throw new RdfRepositoryException("Could not update statement", e);
        }
    }

    /**
     * Add a triple to the repository.
     */
    public void add(Resource subject, IRI predicate, Value object) {
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.add(subject, predicate, object);
            log.debug("Added statement for subject {} predicate {} object {}", subject, predicate, object);
        } catch (Exception e) {
            log.error("Failed to add statement for subject {} predicate {} object {}", subject, predicate, object, e);
            throw new RdfRepositoryException("Could not add statement", e);
        }
    }

    /**
     * Get statements matching the pattern. Caller is responsible for closing the result and connection.
     */
    public RepositoryResult<Statement> getStatements(Resource resource, IRI predicate, Value object) {
        RepositoryConnection conn = repository.getConnection();
        return conn.getStatements(resource, predicate, object);
        // Note: The caller is responsible for closing the RepositoryResult and connection.
    }

    /**
     * Execute a batch of operations in a single transaction.
     * The consumer receives a RepositoryConnection and can perform multiple operations.
     * The transaction is committed on success and rolled back on failure.
     */
    @org.springframework.transaction.annotation.Transactional
    public void executeInTransaction(Consumer<RepositoryConnection> batchOperations) {
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.begin();
            log.debug("Transaction started for batch operation");
            try {
                batchOperations.accept(conn);
                conn.commit();
                log.debug("Transaction committed for batch operation");
            } catch (Exception e) {
                conn.rollback();
                log.error("Transaction rolled back due to error in batch operation", e);
                throw new RdfRepositoryException("Batch operation failed and was rolled back", e);
            }
        } catch (Exception e) {
            log.error("Failed to execute batch operation in transaction", e);
            throw new RdfRepositoryException("Could not execute batch operation", e);
        }
    }
}
