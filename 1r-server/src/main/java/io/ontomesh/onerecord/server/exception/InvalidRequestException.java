package io.ontomesh.onerecord.server.exception;

public class InvalidRequestException extends RuntimeException {
    private String fieldName;

    public InvalidRequestException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    public InvalidRequestException(String message) {
        super(message);
    }
}
