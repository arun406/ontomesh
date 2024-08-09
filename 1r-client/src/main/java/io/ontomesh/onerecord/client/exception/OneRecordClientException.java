package io.ontomesh.onerecord.client.exception;

public class OneRecordClientException extends RuntimeException {
    private int statusCode;

    public OneRecordClientException(int statusCode) {
        this.statusCode = statusCode;
    }

    public OneRecordClientException(String message) {
        super(message);
    }
}
