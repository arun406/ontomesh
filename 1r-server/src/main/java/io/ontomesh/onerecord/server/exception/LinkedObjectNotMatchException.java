package io.ontomesh.onerecord.server.exception;

import org.eclipse.rdf4j.model.IRI;

public class LinkedObjectNotMatchException extends RuntimeException {
    private IRI iri;

    public LinkedObjectNotMatchException() {
    }

    public LinkedObjectNotMatchException(String s) {
        super(s);
    }

    public LinkedObjectNotMatchException(IRI linkedObjectId, String s) {
        super(s);
        this.iri = linkedObjectId;
    }

    public IRI getIri() {
        return this.iri;
    }
}
