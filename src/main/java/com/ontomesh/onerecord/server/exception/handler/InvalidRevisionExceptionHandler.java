package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.domain.ErrorObject;
import com.ontomesh.onerecord.server.exception.InvalidRevisionException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ontomesh.onerecord.server.model.ErrorModelsRepo.hasDetails;

@RestControllerAdvice
public class InvalidRevisionExceptionHandler extends AbstractBadRequestExceptionHandler<InvalidRevisionException> {

    @Override
    public Model getErrorModel(InvalidRevisionException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Object revision does not match"))
                .add(hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(API.hasMessage, "LogisticsObject revision in Change does not match requested Logistics Object revision")
                .add(API.hasResource, getResource(exception))
                .build();
    }

    @ExceptionHandler(value = InvalidRevisionException.class)
    public ErrorObject handle(InvalidRevisionException exception) {
        return super.handle(exception);
    }

    public String getResource(InvalidRevisionException exception) {
        return exception.getIri().stringValue();
    }
}
