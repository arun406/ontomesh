package io.ontomesh.onerecord.server.exception;

public class UnimplementedRequestProcessor extends RuntimeException {
    public UnimplementedRequestProcessor(String message) {
        super(message);
    }
}
