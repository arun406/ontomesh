package io.ontomesh.onerecord.server.exception.handler;

import io.ontomesh.onerecord.server.domain.ErrorObject;
import io.ontomesh.onerecord.server.exception.LogisticsEventNotFoundException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LogisticsEventNotFoundExceptionHandler extends AbstractBadRequestExceptionHandler<LogisticsEventNotFoundException> {

    @Override
    public Model getErrorModel(LogisticsEventNotFoundException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        ModelBuilder builder = new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Event not found"))
                .add(API.hasErrorDetail, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(API.hasMessage, exception.getMessage());
        
        // Add resource IRI if available
        if (exception.getIri() != null) {
            builder.add(API.hasResource, exception.getIri().stringValue());
        }
        
        return builder.build();
    }

    @ExceptionHandler(value = LogisticsEventNotFoundException.class)
    public ErrorObject handle(LogisticsEventNotFoundException exception) {
        return super.handle(exception);
    }
}
