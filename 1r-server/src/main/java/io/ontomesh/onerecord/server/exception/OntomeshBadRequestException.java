package io.ontomesh.onerecord.server.exception;

/**
 * Exception thrown to indicate a client-side (HTTP 400 Bad Request) error in Ontomesh REST controllers.
 */
public class OntomeshBadRequestException extends RuntimeException {

    /**
     * Constructs a new OntomeshBadRequestException with the specified detail message.
     *
     * @param message the detail message
     */
    public OntomeshBadRequestException(String message) {
        super(message);
    }

    /**
     * Constructs a new OntomeshBadRequestException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public OntomeshBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new OntomeshBadRequestException with the specified cause.
     *
     * @param cause the cause
     */
    public OntomeshBadRequestException(Throwable cause) {
        super(cause);
    }
} 