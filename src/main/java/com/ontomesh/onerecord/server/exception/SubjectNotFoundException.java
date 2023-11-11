package com.ontomesh.onerecord.server.exception;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.rdf4j.model.IRI;

@Data
@NoArgsConstructor
public class SubjectNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private IRI iri;

    public SubjectNotFoundException(String s) {
        super(s);
    }
}
