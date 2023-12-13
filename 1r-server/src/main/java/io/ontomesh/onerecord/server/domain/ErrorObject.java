package io.ontomesh.onerecord.server.domain;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.springframework.http.HttpStatus;

public record ErrorObject(IRI iri, Model errorModel, HttpStatus httpStatus) {
}
