package io.ontomesh.onerecord.server.exception;

import lombok.Data;
import org.eclipse.rdf4j.model.IRI;

@Data
public class AlreadyExistsException extends RuntimeException {
    private IRI iri;
    private String loType;

    public AlreadyExistsException(IRI iri, String loType) {
        this.iri = iri;
        this.loType = loType;
    }
}
