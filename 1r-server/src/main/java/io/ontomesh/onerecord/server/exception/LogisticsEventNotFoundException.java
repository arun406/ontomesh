package io.ontomesh.onerecord.server.exception;

import lombok.Getter;
import org.eclipse.rdf4j.model.IRI;

/**
 * Exception thrown when a Logistics Event is not found in the repository.
 */
@Getter
public class LogisticsEventNotFoundException extends RuntimeException {
    
    private final IRI iri;
    
    public LogisticsEventNotFoundException(String message) {
        super(message);
        this.iri = null;
    }
    
    public LogisticsEventNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.iri = null;
    }
    
    public LogisticsEventNotFoundException(Throwable cause) {
        super(cause);
        this.iri = null;
    }
    
    public LogisticsEventNotFoundException(IRI iri) {
        super("Logistics Event not found with IRI: " + iri);
        this.iri = iri;
    }
    
    public LogisticsEventNotFoundException(IRI iri, String message) {
        super(message);
        this.iri = iri;
    }
}
