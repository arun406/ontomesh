package io.ontomesh.onerecord.server.exception;

import org.eclipse.rdf4j.model.IRI;

public class InvalidAddressException extends RuntimeException {
    public final IRI iri;
    public final IRI baseIRI;

    public InvalidAddressException(IRI iri, IRI baseIRI) {
        super();
        this.iri = iri;
        this.baseIRI = baseIRI;
    }
}
