package com.ontomesh.onerecord.server.exception;

import lombok.Data;
import org.eclipse.rdf4j.model.IRI;

@Data
public class InvalidChangeOperationException extends RuntimeException {
    private IRI iri;
}
