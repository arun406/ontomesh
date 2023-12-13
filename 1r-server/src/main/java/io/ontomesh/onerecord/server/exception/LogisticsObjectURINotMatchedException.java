package io.ontomesh.onerecord.server.exception;

import lombok.Data;
import org.eclipse.rdf4j.model.IRI;

@Data
public class LogisticsObjectURINotMatchedException extends RuntimeException {
    private IRI iri;
}
