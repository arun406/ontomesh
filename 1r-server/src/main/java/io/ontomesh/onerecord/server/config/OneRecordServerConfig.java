package io.ontomesh.onerecord.server.config;

import io.ontomesh.onerecord.server.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.converter.StringToModelConverter;
import io.ontomesh.onerecord.server.handler.DefaultStatementHandler;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OneRecordServerConfig {

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