package io.ontomesh.onerecord.server.repository;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Data
public abstract class BaseRepository {

    @Autowired
    public RepositoryConnection connection;

    /**
     * @param statements
     * @param newStatements
     * @param continueTraversal
     * @return
     */
    private List<Statement> traverse(List<Statement> statements, List<Statement> newStatements,
                                     Predicate<Statement> continueTraversal) {

        statements.forEach(statement -> {
            if (newStatements.contains(statement)) {
                return;
            }
            newStatements.add(statement);
            Value value = statement.getObject();
            if (value.isIRI()) {
                // add @type when by default.
                RepositoryResult<Statement> result = connection.getStatements((Resource) value, RDF.TYPE, null);
                if (result.hasNext()) {
                    Statement next = result.next();
                    if (next != null) {
                        newStatements.add(next);
                    }
                }
                if (continueTraversal.test(statement)) {
                    RepositoryResult<Statement> bNodeStatements = connection
                            .getStatements((Resource) value, null, null);

                    traverse(bNodeStatements.stream().toList(), newStatements, continueTraversal);
                }
            }
        });

        return newStatements;
    }

    public void persist(Model model) {
        this.connection.add(model);
    }

    /**
     * @param iri
     * @param continueRecursionIf
     * @return
     */
    public Optional<Model> findByIRI(IRI iri, Predicate<Statement> continueRecursionIf) {
        RepositoryResult<Statement> statements = connection.getStatements(iri, null, null);
        List<Statement> baseStatements = statements.stream().toList();
        if (baseStatements.isEmpty()) {
            return Optional.empty();
        }
        return findRecursively(continueRecursionIf, baseStatements);
    }

    /**
     * @param continueRecursionIf
     * @param statements
     * @return
     */
    public Optional<Model> findRecursively(Predicate<Statement> continueRecursionIf, List<Statement> statements) {

        Model model = new DynamicModelFactory().createEmptyModel();
        model.addAll(statements);
        List<Statement> list = traverse(statements, new ArrayList<>(), continueRecursionIf);
        model.addAll(list);
        return Optional.of(model);
    }

    public int delete(Resource subject, IRI predicate, Value object) {
        int affectedTriples = QueryResults.asList(
                connection.getStatements(subject, predicate, object, true)
        ).size();

        connection.remove(subject, predicate, object);

        return affectedTriples;
    }

    public void update(Resource subject, IRI predicate, Value oldValue, Value newValue) {
        log.debug("updating the request status from {} to new request status {}", oldValue, newValue);
        delete(subject, predicate, oldValue);
        add(subject, predicate, newValue);
    }

    public void add(Resource subject, IRI predicate, Value object) {
        connection.add(subject, predicate, object);
    }

    public RepositoryResult<Statement> getStatements(Resource resource, IRI predicate, Value object) {
        return connection.getStatements(resource, predicate, object);
    }
}
