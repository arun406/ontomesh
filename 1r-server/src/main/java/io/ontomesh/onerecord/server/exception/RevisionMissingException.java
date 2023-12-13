package io.ontomesh.onerecord.server.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.rdf4j.model.IRI;

@AllArgsConstructor
@Data
public class RevisionMissingException extends RuntimeException {
    private IRI iri;
}
