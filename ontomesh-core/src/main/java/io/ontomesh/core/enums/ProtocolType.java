package io.ontomesh.core.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final Map<String, ProtocolType> PROTOCOL_TYPE_MAP = Arrays.stream(ProtocolType.values())
            .collect(Collectors.toMap(ProtocolType::getValue, Function.identity()));

    public static ProtocolType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return PROTOCOL_TYPE_MAP.get(value);
    }
}
