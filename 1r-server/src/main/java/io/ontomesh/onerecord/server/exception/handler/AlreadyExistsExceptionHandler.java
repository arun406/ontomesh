package io.ontomesh.onerecord.server.exception.handler;

import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.ErrorObject;
import io.ontomesh.onerecord.server.exception.AlreadyExistsException;
import io.ontomesh.onerecord.server.handler.AbstractExceptionHandler;
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
public class AlreadyExistsExceptionHandler extends AbstractExceptionHandler<AlreadyExistsException> {

    public static final String ID = "@id";

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public Model getErrorModel(AlreadyExistsException exception, IRI errorIRI, IRI errorDetailsIRI,
                               HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistic Object Already Exists"))
                .add(OntoMesh.hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(OntoMesh.hasAttribute, ID)
                .add(API.hasResource, exception.getIri().stringValue())
                .add(API.hasMessage, exception.getIri().toString() + " is already exists.")
                .build();
    }

    @ExceptionHandler(value = {AlreadyExistsException.class})
    public ErrorObject handle(AlreadyExistsException exception) {
        return super.handle(exception);
    }
}
