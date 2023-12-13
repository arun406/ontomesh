package io.ontomesh.onerecord.server.exception;

public class SnapshotNotFoundException extends RuntimeException {
    public SnapshotNotFoundException() {
        super();
    }

    public SnapshotNotFoundException(String message) {
        super(message);
    }
}
