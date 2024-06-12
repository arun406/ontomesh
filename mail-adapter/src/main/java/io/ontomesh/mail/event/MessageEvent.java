package io.ontomesh.mail.event;

import io.ontomesh.mail.enums.MessageEventType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Setter
@Getter
public class MessageEvent<T> implements Serializable {

    private String id;
    private MessageEventType type;
    private Map<String, String> properties;
    private T payload;
}
