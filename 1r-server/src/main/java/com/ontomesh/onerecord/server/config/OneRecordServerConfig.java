package com.ontomesh.onerecord.server.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
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
}