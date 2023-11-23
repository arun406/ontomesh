package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.domain.ErrorObject;
import com.ontomesh.onerecord.server.exception.LogisticsObjectNotSupportedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ontomesh.onerecord.server.common.OntoMesh.hasAttribute;
import static com.ontomesh.onerecord.server.common.OntoMesh.hasDetails;

@RestControllerAdvice
public class LogisticsObjectNotSupportedExceptionHandler extends AbstractBadRequestExceptionHandler<LogisticsObjectNotSupportedException> {

    @Override
    public Model getErrorModel(LogisticsObjectNotSupportedException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Object Type not supported"))
                .add(hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(hasAttribute, "topic")
                .add(API.hasMessage, getMessage(exception))
                .build();
    }

    private static String getMessage(LogisticsObjectNotSupportedException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(value = LogisticsObjectNotSupportedException.class)
    public ErrorObject handle(LogisticsObjectNotSupportedException exception) {
        return super.handle(exception);
    }
}
