package io.ontomesh.mail.dto;

import io.ontomesh.mail.enums.MessageStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public abstract class Message implements Serializable {

    private String to;
    private String body;
    private MessageStatus status;
}
