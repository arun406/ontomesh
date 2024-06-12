package io.ontomesh.mail.processor;

import io.ontomesh.mail.event.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MessageEventProcessor {

    public void process(Map<?, ?> headers, MessageEvent<?> payload) {
        log.debug("message processed successfully....");
    }
}
