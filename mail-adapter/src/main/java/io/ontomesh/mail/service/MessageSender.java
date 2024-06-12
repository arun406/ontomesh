package io.ontomesh.mail.service;

import io.ontomesh.mail.enums.MessageEventType;
import io.ontomesh.mail.event.MessageEvent;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class MessageSender<T, I, R extends JpaRepository<T, I>> {

    @Autowired
    private StreamBridge streamBridge;

    @Transactional
    public T send(R repository, T entity) {
        //persists to database.
        T save = repository.save(entity);
        // send to kafka
        MessageEvent<T> event = new MessageEvent<>();
        event.setId(UUID.randomUUID().toString());
        event.setType(MessageEventType.CREATED);
        event.setPayload(entity);

        this.streamBridge.send("message-out-0", event);

        return save;
    }
}
