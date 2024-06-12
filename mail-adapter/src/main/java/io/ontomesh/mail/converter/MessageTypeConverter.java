package io.ontomesh.mail.converter;

import io.ontomesh.mail.enums.MessageType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class MessageTypeConverter implements AttributeConverter<MessageType, String> {
    @Override
    public String convertToDatabaseColumn(MessageType messageType) {
        if (messageType == null) {
            return null;
        }
        return messageType.getCode();
    }

    @Override
    public MessageType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }

        return Stream.of(MessageType.values())
                .filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
