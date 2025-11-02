package io.ontomesh.onerecord.server.exception;

import lombok.Getter;
import org.eclipse.rdf4j.model.IRI;

/**
 * Exception thrown when a Logistics Object is not found in the repository.
 */
@Getter
public class LogisticsObjectNotFoundException extends RuntimeException {
    
    private final IRI iri;
    
    public LogisticsObjectNotFoundException(String message) {
        super(message);
        this.iri = null;
    }
    
    public LogisticsObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.iri = null;
    }
    
    public LogisticsObjectNotFoundException(Throwable cause) {
        super(cause);
        this.iri = null;
    }
    
    public LogisticsObjectNotFoundException(IRI iri) {
        super("Logistics Object not found with IRI: " + iri);
        this.iri = iri;
    }
    
    public LogisticsObjectNotFoundException(IRI iri, String message) {
        super(message);
        this.iri = iri;
    }
}
