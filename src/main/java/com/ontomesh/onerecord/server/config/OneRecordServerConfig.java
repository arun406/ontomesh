package com.ontomesh.onerecord.server.config;

import com.ontomesh.onerecord.server.util.ModelToStringConverter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OneRecordServerConfig {

    @Bean
    public Repository repository() {
        return new SailRepository(new MemoryStore());
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