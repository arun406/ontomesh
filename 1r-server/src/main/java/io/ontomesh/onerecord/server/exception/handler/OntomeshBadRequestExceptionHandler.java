package io.ontomesh.onerecord.server.exception.handler;

import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.ErrorObject;
import io.ontomesh.onerecord.server.exception.OntomeshBadRequestException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles OntomeshBadRequestException and returns a structured error response with HTTP 400.
 */
@Order(2)
@RestControllerAdvice
public class OntomeshBadRequestExceptionHandler extends AbstractBadRequestExceptionHandler<OntomeshBadRequestException> {

    private static String getMessage(OntomeshBadRequestException exception) {
        return exception.getMessage();
    }

    @Override
    public Model getErrorModel(OntomeshBadRequestException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Bad Request"))
                .add(API.hasErrorDetail, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(OntoMesh.hasAttribute, "bad_request")
                .add(API.hasMessage, getMessage(exception))
                .build();
    }

    @ExceptionHandler(value = OntomeshBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorObject exception(OntomeshBadRequestException exception) {
        return super.handle(exception);
    }
} 