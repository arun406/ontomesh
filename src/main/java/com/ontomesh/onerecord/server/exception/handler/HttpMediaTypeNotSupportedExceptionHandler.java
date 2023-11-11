package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.domain.ErrorObject;
import com.ontomesh.onerecord.server.handler.AbstractExceptionHandler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ontomesh.onerecord.server.common.OntoMesh.hasAttribute;
import static com.ontomesh.onerecord.server.common.OntoMesh.hasDetails;

@RestControllerAdvice
public class HttpMediaTypeNotSupportedExceptionHandler extends AbstractExceptionHandler<HttpMediaTypeNotSupportedException> {

    private MediaType contentType;

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
    }

    @Override
    public Model getErrorModel(HttpMediaTypeNotSupportedException exception, IRI errorIRI, IRI errorDetailsIRI,
                               HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Media Type Not Supported"))
                .add(hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(hasAttribute, "Content-Type")
                .add(API.hasResource, getResourceType())
                .add(API.hasMessage, "Content-Type '" + getContentType() + "' is not supported.")
                .build();
    }

    private String getResourceType() {
        return "CompanyInformation";
    }

    private String getContentType() {
        return this.contentType.toString();
    }

    @ExceptionHandler(value = {HttpMediaTypeNotSupportedException.class})
    public ErrorObject exception(@RequestHeader(HttpHeaders.CONTENT_TYPE) MediaType contentType,
                                 HttpMediaTypeNotSupportedException exception) {
        this.contentType = contentType;
        return super.handle(exception);
    }
}
