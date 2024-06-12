package io.ontomesh.mail.enums;

public enum MessageStatus {
    SENT("S"), SENDING("P"), NOT_SENT("N"), FAILED("F");

    private final String code;

    private MessageStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
