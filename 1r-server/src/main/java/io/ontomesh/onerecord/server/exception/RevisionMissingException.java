package io.ontomesh.onerecord.server.exception;

import lombok.Getter;
import org.eclipse.rdf4j.model.IRI;

@Getter
public class RevisionMissingException extends RuntimeException {
    private IRI iri;

    public RevisionMissingException() {
    }

    public RevisionMissingException(IRI iri) {
        this.iri = iri;
    }

    public RevisionMissingException(String message, IRI iri) {
        super(message);
        this.iri = iri;
    }
}
