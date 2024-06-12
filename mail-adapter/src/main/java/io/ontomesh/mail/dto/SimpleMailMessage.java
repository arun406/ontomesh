package io.ontomesh.mail.dto;

import io.ontomesh.mail.enums.MessageType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class SimpleMailMessage extends Message implements Serializable {

    private MessageType type = MessageType.MAIL;
    private String cc;
    private String bcc;
    private String subject;
}
