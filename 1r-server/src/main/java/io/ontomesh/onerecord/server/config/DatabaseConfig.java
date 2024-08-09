package io.ontomesh.onerecord.server.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@Configuration
public class DatabaseConfig {

    @Autowired
    private RemoteRepositoryProperties remoteRepositoryProperties;

    @Bean
    public Repository repository() {
        return new HTTPRepository(remoteRepositoryProperties.getManagerUrl(), remoteRepositoryProperties.getName());
    }

    @Bean
    public RepositoryConnection getConnection() {
        RepositoryConnection connection = repository().getConnection();
//        connection.setAutoCommit(false);
        return connection;
    }

}
