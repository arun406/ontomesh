package io.ontomesh.core.rdf.config;

import io.ontomesh.core.properties.ServerProperties;
import io.ontomesh.core.rdf.converter.ModelToStringConverter;
import io.ontomesh.core.rdf.converter.StringToModelConverter;
import io.ontomesh.core.rdf.handler.BlankNodeReplacingStatementHandler;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.spring.operationcache.OperationCacheProperties;
import org.eclipse.rdf4j.spring.operationlog.OperationLogProperties;
import org.eclipse.rdf4j.spring.pool.PoolProperties;
import org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryProperties;
import org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryProperties;
import org.eclipse.rdf4j.spring.resultcache.ResultCacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Rdf4jConfig {

    @Bean
    public RemoteRepositoryProperties remoteRepositoryProperties() {
        return new RemoteRepositoryProperties();
    }

    @Bean
    public InMemoryRepositoryProperties inMemoryRepositoryProperties() {
        return new InMemoryRepositoryProperties();
    }

    /*
    @Bean
    public TxProperties txProperties() {
        return new TxProperties();
    }*/

    @Bean
    public PoolProperties poolProperties() {
        return new PoolProperties();
    }

    @Bean
    public ResultCacheProperties resultCacheProperties() {
        return new ResultCacheProperties();
    }

    @Bean
    public OperationLogProperties operationLogProperties() {
        return new OperationLogProperties();
    }

    @Bean
    public OperationCacheProperties operationCacheProperties() {
        return new OperationCacheProperties();
    }

    @Bean(name = "jsonLDToStringConverter")
    public ModelToStringConverter jsonLDToStringConverter() {
        return new ModelToStringConverter(RDFFormat.JSONLD);
    }

    @Bean(name = "turtleToStringConverter")
    public ModelToStringConverter turtleToStringConverter() {
        return new ModelToStringConverter(RDFFormat.TURTLE);
    }

    @Bean(name = "stringToJsonLDConverter")
    public StringToModelConverter stringToJsonLDConverter(ServerProperties serverProperties) {
        BlankNodeReplacingStatementHandler statementHandler =
                new BlankNodeReplacingStatementHandler(serverProperties.getInternalIriScheme());
        return new StringToModelConverter(RDFFormat.JSONLD, statementHandler);
    }

    @Bean(name = "stringToTurtleConverter")
    public StringToModelConverter stringToTurtleConverter(ServerProperties serverProperties) {
        BlankNodeReplacingStatementHandler statementHandler =
                new BlankNodeReplacingStatementHandler(serverProperties.getInternalIriScheme());

        return new StringToModelConverter(RDFFormat.TURTLE, statementHandler);
    }
}
