package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.domain.ErrorObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RDFParseExceptionHandler extends AbstractBadRequestExceptionHandler<RDFParseException> {
    @Override
    public Model getErrorModel(RDFParseException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder()
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Invalid RDF model"))
                .build();
    }

    @ExceptionHandler(value = {RDFParseException.class})
    public ErrorObject handle(RDFParseException exception) {
        return super.handle(exception);
    }
}
