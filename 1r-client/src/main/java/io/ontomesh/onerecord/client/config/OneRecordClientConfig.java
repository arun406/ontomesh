package io.ontomesh.onerecord.client.config;

import io.ontomesh.onerecord.client.converter.ModelToStringConverter;
import io.ontomesh.onerecord.client.converter.StringToModelConverter;
import io.ontomesh.onerecord.client.handlers.BlankNodeReplacingStatementHandler;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OneRecordClientConfig {

    private final BlankNodeReplacingStatementHandler statementHandler;

    public OneRecordClientConfig(BlankNodeReplacingStatementHandler statementHandler) {
        this.statementHandler = statementHandler;
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
    public StringToModelConverter stringToJsonLDConverter() {
        return new StringToModelConverter(RDFFormat.JSONLD, statementHandler);
    }

    @Bean(name = "stringToTurtleConverter")
    public StringToModelConverter stringToTurtleConverter() {
        return new StringToModelConverter(RDFFormat.TURTLE, statementHandler);
    }
}
