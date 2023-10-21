package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.domain.ErrorObject;
import com.ontomesh.onerecord.server.exception.RevisionMissingException;
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
public class RevisionMissingExceptionHandler extends AbstractBadRequestExceptionHandler<RevisionMissingException> {
    @Override
    public Model getErrorModel(RevisionMissingException exception, IRI errorIRI, IRI errorDetailsIRI,
                               HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Revision is missing in Change request"))
                .add(hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(API.hasMessage, "Revision is missing in Change request")
                .add(API.hasResource, exception.getIri().stringValue())
                .build();
    }


    @ExceptionHandler(value = RevisionMissingException.class)
    public ErrorObject handle(RevisionMissingException exception) {
        return super.handle(exception);
    }
}
