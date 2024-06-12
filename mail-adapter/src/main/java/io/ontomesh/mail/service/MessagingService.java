package io.ontomesh.mail.service;

import io.ontomesh.mail.domain.MessageEntity;
import io.ontomesh.mail.dto.SimpleMailMessage;
import io.ontomesh.mail.mapper.MessageMapper;
import io.ontomesh.mail.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessagingService {

    @Autowired
    private MessageRepository repository;
    @Autowired
    MessageSender<MessageEntity, Long, MessageRepository> sender;

    @Autowired
    private MessageMapper mapper;

    public void send(SimpleMailMessage dto) {
        MessageEntity messageEntity = mapper.toEntity(dto);

        MessageEntity entity = sender.send(repository, messageEntity);
        log.debug("Message  {} send successfully.", entity.getId());
    }
}
