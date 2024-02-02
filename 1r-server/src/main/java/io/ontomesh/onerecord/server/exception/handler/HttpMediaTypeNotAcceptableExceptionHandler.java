package io.ontomesh.onerecord.server.exception.handler;

import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.ErrorObject;
import io.ontomesh.onerecord.server.handler.AbstractExceptionHandler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HttpMediaTypeNotAcceptableExceptionHandler extends AbstractExceptionHandler<HttpMediaTypeNotAcceptableException> {
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_ACCEPTABLE;
    }

    @Override
    public Model getErrorModel(HttpMediaTypeNotAcceptableException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Media Type Not Supported"))
                .add(OntoMesh.hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(OntoMesh.hasAttribute, "Accept")
                .add(API.hasResource, getResourceType())
                .add(API.hasMessage, "Accept '" + getMediaType() + "' is not supported.")
                .build();
    }

    private String getResourceType() {
        return "CompanyInformation"; //TODO
    }

    private String getMediaType() {
        return httpServletRequest.getHeader(HttpHeaders.ACCEPT);

    }

    @ExceptionHandler(value = {HttpMediaTypeNotAcceptableException.class})
    public ErrorObject handle(HttpMediaTypeNotAcceptableException exception) {
        return super.handle(exception);
    }
}
