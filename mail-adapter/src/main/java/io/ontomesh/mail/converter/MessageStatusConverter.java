package io.ontomesh.mail.converter;

import io.ontomesh.mail.enums.MessageStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class MessageStatusConverter implements AttributeConverter<MessageStatus, String> {
    @Override
    public String convertToDatabaseColumn(MessageStatus messageStatus) {
        if (messageStatus == null) {
            return null;
        }
        return messageStatus.getCode();
    }

    @Override
    public MessageStatus convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        return Stream.of(MessageStatus.values()).filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
