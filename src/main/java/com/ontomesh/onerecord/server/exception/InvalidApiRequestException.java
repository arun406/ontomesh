package com.ontomesh.onerecord.server.exception;

public class InvalidApiRequestException extends RuntimeException {
    public InvalidApiRequestException() {
    }

    public InvalidApiRequestException(String message) {
        super(message);
    }
}
