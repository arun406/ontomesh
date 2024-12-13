package io.ontomesh.onerecord.server.exception.handler;

import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.ErrorObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MissingServletRequestParameterExceptionHandler extends AbstractBadRequestExceptionHandler<MissingServletRequestParameterException> {

    private static String getMessage(MissingServletRequestParameterException exception) {
        return exception.getMessage();
    }

    @Override
    public Model getErrorModel(MissingServletRequestParameterException exception, IRI errorIRI, IRI errorDetailsIRI, HttpStatus httpStatus) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Invalid API Request."))
                .add(API.hasErrorDetail, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, httpStatus.value())
                .add(OntoMesh.hasAttribute, "topic")
                .add(API.hasMessage, getMessage(exception))
                .build();
    }

    @ExceptionHandler(value = {MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorObject exception(MissingServletRequestParameterException exception) {
        return super.handle(exception);
    }

}
