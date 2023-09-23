package com.ontomesh.onerecord.server.repository;

import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.exception.SubjectNotFoundException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
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

    public void persist(IRI iri, Model model) {
        this.connection.add(model);
    }


    /**
     * @param iri
     * @return
     */
    public Optional<Model> findByIRI(IRI iri) {
        Predicate<IRI> continueRecursionIf = OntoMeshId::isInternalIri;
        log.info("continueRecursionIf: {}", continueRecursionIf);
        RepositoryResult<Statement> statements = connection.getStatements(iri, null, null);
        List<Statement> baseStatements = statements.stream().toList();
        if (baseStatements.isEmpty()) {
            throw new SubjectNotFoundException();
        }
        return findRecursively(continueRecursionIf, baseStatements);
    }

    /**
     * @param continueRecursionIf
     * @param statements
     * @return
     */
    public Optional<Model> findRecursively(Predicate<IRI> continueRecursionIf,
                                           List<Statement> statements) {

        Model model = new DynamicModelFactory().createEmptyModel();
        model.addAll(statements);
        List<Statement> list = traverse(statements, new ArrayList<>(), continueRecursionIf);
        model.addAll(list);
        return Optional.of(model);
    }

    /**
     * @param statements
     * @param newStatements
     * @param continueTraversal
     * @return
     */
    private List<Statement> traverse(List<Statement> statements, List<Statement> newStatements, Predicate<IRI> continueTraversal) {
        statements.forEach(statement -> {
            if (newStatements.contains(statement)) {
                return;
            }
            newStatements.add(statement);
            if (statement.getObject().isIRI() && continueTraversal.test((IRI) statement.getObject())) {
                RepositoryResult<Statement> bNodeStatements = connection
                        .getStatements((Resource) statement.getObject(), null, null);
                traverse(bNodeStatements.stream().toList(), newStatements, continueTraversal);
            }
        });
        return newStatements;
    }

    public int delete(Resource subject, IRI predicate, Value object) {
        int affectedTriples = QueryResults.asList(
                connection.getStatements(subject, predicate, object, true)
        ).size();

        connection.remove(subject, predicate, object);

        return affectedTriples;
    }

    public void add(Resource subject, IRI predicate, Value object) {
        connection.add(subject, predicate, object);
    }

    public RepositoryResult<Statement> getStatements(Resource resource, IRI predicate, Value object) {
        return connection.getStatements(resource, predicate, object);
    }
}
