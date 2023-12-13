package com.ontomesh.onerecord.server.config;

import com.ontomesh.onerecord.server.converter.ModelToStringConverter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OneRecordServerConfig {

    @Bean
    public Repository repository() {
//        return new SailRepository(new MemoryStore());
        String rdf4jServer = "http://localhost:8080/rdf4j-server/";
        String repositoryID = "ontomesh";
        return new HTTPRepository(rdf4jServer, repositoryID);
    }

    @Bean
    public RepositoryConnection getConnection() {
        return repository().getConnection();
    }

    @Bean("modelToJsonLdStringConverter")
    public ModelToStringConverter modelToJsonLdStringConverter() {
        return new ModelToStringConverter(RDFFormat.JSONLD);
    }

    @Bean("modelToTurtleStringConverter")
    public ModelToStringConverter modelToTurtleStringConverter() {
        return new ModelToStringConverter(RDFFormat.TURTLE);
    }
}