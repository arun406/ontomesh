package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.domain.ErrorObject;
import com.ontomesh.onerecord.server.exception.InvalidChangeOperationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ontomesh.onerecord.server.common.CommonConstants.hasDetails;

@RestControllerAdvice
public class InvalidChangeOperationExceptionHandler extends AbstractBadRequestExceptionHandler<InvalidChangeOperationException> {

    @Override
    public Model getErrorModel(InvalidChangeOperationException exception, IRI errorIRI, IRI errorDetailsIRI,
                               HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Events can not be updated"))
                .add(hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(API.hasMessage, "Logistics Events of a Logistics Object can not be updated using PATCH")
                .add(API.hasResource, getResource(exception))
                .build();
    }

    private String getResource(InvalidChangeOperationException exception) {
        return exception.getIri().stringValue();
    }

    @ExceptionHandler(value = InvalidChangeOperationException.class)
    public ErrorObject handle(InvalidChangeOperationException exception) {
        return super.handle(exception);
    }
}
