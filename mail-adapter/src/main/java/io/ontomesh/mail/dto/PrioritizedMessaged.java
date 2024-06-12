package io.ontomesh.mail.dto;

import io.ontomesh.mail.enums.MessagePriority;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PrioritizedMessaged extends Message {
    private MessagePriority priority;
}
