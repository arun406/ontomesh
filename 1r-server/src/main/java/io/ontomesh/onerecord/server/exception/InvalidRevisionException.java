package io.ontomesh.onerecord.server.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.rdf4j.model.IRI;

@Data
@AllArgsConstructor
public class InvalidRevisionException extends RuntimeException {
    private IRI iri;
}
