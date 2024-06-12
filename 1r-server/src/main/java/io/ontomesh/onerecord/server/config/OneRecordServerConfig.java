package io.ontomesh.onerecord.server.config;

import io.ontomesh.onerecord.server.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.converter.StringToModelConverter;
import io.ontomesh.onerecord.server.handler.DefaultStatementHandler;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OneRecordServerConfig {


    @Value("${rdf4j.spring.repository.remote.manager-url:http://localhost:8080/rdf4j-server/}")
    private String rdf4jServer;

    @Value("${rdf4j.spring.repository.remote.name:ontomesh}")
    private String repositoryID;

    @Bean
    public Repository repository() {
//        return new SailRepository(new MemoryStore());
//        String rdf4jServer = "http://localhost:9090/rdf4j-server/";
//        String repositoryID = "ontomesh";
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

    @Bean("stringToJsonLdConverter")
    public StringToModelConverter stringToJsonLdConverter(DefaultStatementHandler statementHandler) {
        return new StringToModelConverter(RDFFormat.JSONLD, statementHandler);
    }

    @Bean("stringToTurtleConverter")
    public StringToModelConverter stringToTurtleConverter(DefaultStatementHandler statementHandler) {
        return new StringToModelConverter(RDFFormat.TURTLE, statementHandler);
    }

}