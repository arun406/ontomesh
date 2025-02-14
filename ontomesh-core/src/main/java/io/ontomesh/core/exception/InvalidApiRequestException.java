package io.ontomesh.core.exception;


public class InvalidApiRequestException extends RuntimeException {
    public InvalidApiRequestException() {
    }

    public InvalidApiRequestException(String message) {
        super(message);
    }
}
