package io.ontomesh.onerecord.server.exception;

import lombok.Data;
import org.eclipse.rdf4j.model.IRI;

@Data
public class InvalidRevisionException extends RuntimeException {
    private IRI iri;

    public InvalidRevisionException(IRI iri) {
        this.iri = iri;
    }

    public InvalidRevisionException(String message, IRI iri) {
        super(message);
        this.iri = iri;
    }
}
