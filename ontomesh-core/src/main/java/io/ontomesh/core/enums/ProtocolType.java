package io.ontomesh.core.enums;

public enum ProtocolType {
    EMAIL("email"),
    SMS("sms"),
    HTTP("http"),
    HTTPS("https"),
    ;

    private String m_value;

    ProtocolType(String name) {
        this.m_value = name;
    }

    public String getValue() {
        return m_value;
    }

    public void setValue(String value) {
        this.m_value = value;
    }
}
