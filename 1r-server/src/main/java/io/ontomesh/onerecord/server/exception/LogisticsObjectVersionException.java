package io.ontomesh.onerecord.server.exception;

/**
 * Exception thrown when there are issues with Logistics Object version operations.
 */
public class LogisticsObjectVersionException extends RuntimeException {
    
    public LogisticsObjectVersionException(String message) {
        super(message);
    }
    
    public LogisticsObjectVersionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LogisticsObjectVersionException(Throwable cause) {
        super(cause);
    }
} 