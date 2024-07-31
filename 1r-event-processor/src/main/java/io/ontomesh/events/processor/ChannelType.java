package io.ontomesh.events.processor;

public enum ChannelType {
    EMAIL("EMail"),
    SMS("Sms"),
    HTTP("http"),
    ;

    private String m_name;

    ChannelType(String name) {
        this.m_name = name;
    }

    public String getName() {
        return this.m_name;
    }
}
