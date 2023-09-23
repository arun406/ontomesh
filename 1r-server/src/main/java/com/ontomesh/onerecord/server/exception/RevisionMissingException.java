package com.ontomesh.onerecord.server.exception;

public class RevisionMissingException extends RuntimeException {
    public RevisionMissingException() {
    }

    public RevisionMissingException(String message) {
        super(message);
    }
}
