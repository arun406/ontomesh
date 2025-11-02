package io.ontomesh.onerecord.server.exception;

/**
 * Exception thrown when there are issues with Logistics Object query operations.
 */
public class LogisticsObjectQueryException extends RuntimeException {
    
    public LogisticsObjectQueryException(String message) {
        super(message);
    }
    
    public LogisticsObjectQueryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LogisticsObjectQueryException(Throwable cause) {
        super(cause);
    }
} 