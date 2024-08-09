package io.ontomesh.onerecord.client.messaging;

import io.ontomesh.core.enums.ProtocolType;
import io.ontomesh.onerecord.client.service.INotificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class NotificationMessageConsumer {

    @Autowired
    private INotificationClient notificationClient;

    @Bean
    public Consumer<Message<String>> notificationConsumer() {
        return (Message<String> message) -> {
            MessageHeaders headers = message.getHeaders();
            logHeaders(headers);
            String to = getHeader(headers, "to");
            ProtocolType protocolType = null;
            String protocol = getHeader(headers, "protocol");
            for (ProtocolType temp : ProtocolType.values()) {
                if (protocolType.getValue().equals(protocol)) {
                    protocolType = temp;
                    break;
                }
            }
            if (protocolType == ProtocolType.EMAIL) {
                notificationClient.sendNotification(to, message.getPayload());
            }
        };
    }

    private static String getHeader(MessageHeaders header, Object key) {
        return (String.valueOf(header.get(key)));
    }

    private static void logHeaders(MessageHeaders headers) {
        for (String key : headers.keySet()) {
            String value = getHeader(headers, key);
            log.info("Header Key {}, value : {}", key, value);
        }
    }
}
