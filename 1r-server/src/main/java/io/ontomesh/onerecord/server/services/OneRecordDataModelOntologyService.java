package io.ontomesh.onerecord.server.services;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OneRecordDataModelOntologyService {
    public static final String LOGISTICS_OBJECT_TYPES = """
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
    private final Repository repository;

    public OneRecordDataModelOntologyService() throws IOException {
        // Create an in-memory repository for storing the ontology model
        this.repository = new SailRepository(new MemoryStore());
        this.repository.init();

        try (RepositoryConnection connection = repository.getConnection();
             InputStream inputStream = getClass().getClassLoader().getResourceAsStream("IATA-1R-DM-Ontology.ttl")) {

            if (inputStream == null) {
                throw new RuntimeException("File 'IATA-1R-DM-Ontology.ttl' not found in resources folder");
            }
            // Load the Turtle file into the repository
            connection.add(inputStream, "", RDFFormat.TURTLE);
        }
    }

    /**
     * Retrieves the set of logistics object types from the ontology.
     *
     * @return a set of IRIs representing the logistics object types
     */
    public Set<IRI> logisticsObjectTypes() {
        try (RepositoryConnection connection = this.repository.getConnection()) {
            TupleQuery tupleQuery = connection.prepareTupleQuery(LOGISTICS_OBJECT_TYPES);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                return QueryResults.stream(result)
                        .flatMap(bindingSet -> {
                            Iterable<Binding> iterable = bindingSet::iterator;
                            return StreamSupport.stream(iterable.spliterator(), false);
                        })
                        .filter(binding -> binding.getValue() instanceof IRI)
                        .map(binding -> (IRI) binding.getValue())
                        .collect(Collectors.toSet());
            }
        }
    }
}
