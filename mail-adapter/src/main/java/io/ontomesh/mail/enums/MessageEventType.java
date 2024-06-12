package io.ontomesh.mail.enums;

public enum MessageEventType {
    CREATED("C"), PUBLISHED("P"), REVOKED("R"), FAILED("F");

    private final String code;
    private MessageEventType(String code){
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
