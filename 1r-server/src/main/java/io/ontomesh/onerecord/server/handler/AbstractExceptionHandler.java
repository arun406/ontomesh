package io.ontomesh.onerecord.server.handler;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.ErrorObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public abstract class AbstractExceptionHandler<T> {

    @Autowired
    public IdProvider idProvider;

    @Autowired
    public HttpServletRequest httpServletRequest;

    @Autowired
    public HttpServletResponse httpServletResponse;

    protected AbstractExceptionHandler() {
    }

    public ErrorObject handle(T exception) {
        IRI errorIRI = idProvider.createInternalIri().iri();
        IRI errorDetailsIRI = idProvider.createInternalIri().iri();
        Model errorModel = getErrorModel(exception, errorIRI, errorDetailsIRI, getHttpStatus());
        return new ErrorObject(errorIRI, errorModel, getHttpStatus());
    }

    public abstract HttpStatus getHttpStatus();

    public abstract Model getErrorModel(T exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus);
}
