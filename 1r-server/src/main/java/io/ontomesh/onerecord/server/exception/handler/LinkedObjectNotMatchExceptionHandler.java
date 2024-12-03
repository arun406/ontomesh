package io.ontomesh.onerecord.server.exception.handler;

import io.ontomesh.onerecord.server.domain.ErrorObject;
import io.ontomesh.onerecord.server.exception.LinkedObjectNotMatchException;
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
public class LinkedObjectNotMatchExceptionHandler extends AbstractBadRequestExceptionHandler<LinkedObjectNotMatchException> {

    @Override
    public Model getErrorModel(LinkedObjectNotMatchException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Event not found"))
                .add(API.hasErrorDetail, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(API.hasMessage, exception.getMessage())
                .add(API.hasResource, exception.getIri().stringValue())
                .build();
    }

    @ExceptionHandler(value = LinkedObjectNotMatchException.class)
    public ErrorObject handle(LinkedObjectNotMatchException exception) {
        return super.handle(exception);
    }
}
