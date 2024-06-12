package io.ontomesh.mail.enums;

public enum MessageType {
    MAIL("M"), SMS("S"), PUSH_NOTIFICATION("P");

    private final String code;

    private MessageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
