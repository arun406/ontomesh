package io.ontomesh.mail.mapper;

import io.ontomesh.mail.domain.MessageEntity;
import io.ontomesh.mail.dto.SimpleMailMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    MessageEntity toEntity(SimpleMailMessage dto);

    SimpleMailMessage fromEntity(MessageEntity entity);

}
