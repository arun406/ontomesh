package io.ontomesh.mail.converter;

import io.ontomesh.mail.enums.MessagePriority;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class MessagePriorityConverter implements AttributeConverter<MessagePriority, String> {
    @Override
    public String convertToDatabaseColumn(MessagePriority messagePriority) {
        if (messagePriority == null) {
            return null;
        }
        return messagePriority.getCode();
    }

    @Override
    public MessagePriority convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }

        return Stream.of(MessagePriority.values())
                .filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
