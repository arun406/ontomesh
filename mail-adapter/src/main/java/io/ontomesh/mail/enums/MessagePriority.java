package io.ontomesh.mail.enums;

public enum MessagePriority {
    LOW("L"), MEDIUM("M"), HIGH("H");
    private final String code;

    private MessagePriority(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
