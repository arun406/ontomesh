package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.domain.ErrorObject;
import com.ontomesh.onerecord.server.exception.LogisticsObjectURINotMatchedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ontomesh.onerecord.server.common.OntoMesh.hasDetails;

@RestControllerAdvice
public class LogisticsObjectURINotMatchedExceptionHandler extends AbstractBadRequestExceptionHandler<LogisticsObjectURINotMatchedException> {

    @Override
    public Model getErrorModel(LogisticsObjectURINotMatchedException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Object URI does not match"))
                .add(hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(API.hasMessage, "LogisticsObject URI in Change does not match requested Logistics Object URI")
                .add(API.hasResource, getResource(exception))
                .build();
    }

    private String getResource(LogisticsObjectURINotMatchedException exception) {
        return exception.getIri().stringValue();
    }

    @ExceptionHandler(value = LogisticsObjectURINotMatchedException.class)
    public ErrorObject handler(LogisticsObjectURINotMatchedException exception) {
        return super.handle(exception);
    }
}
