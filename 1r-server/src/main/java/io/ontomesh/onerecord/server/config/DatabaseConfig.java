package io.ontomesh.onerecord.server.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@Configuration
public class DatabaseConfig {

    @Value("${rdf4j.spring.repository.remote.manager-url:http://localhost:8080/rdf4j-server/}")
    private String rdf4jServer;

    @Value("${rdf4j.spring.repository.remote.name:ontomesh}")
    private String repositoryID;

    @Bean
    public Repository repository() {
        return new HTTPRepository(rdf4jServer, repositoryID);
    }

    @Bean
    public RepositoryConnection getConnection() {
        RepositoryConnection connection = repository().getConnection();
        connection.setAutoCommit(false);
        return connection;
    }
}
