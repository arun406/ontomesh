package io.ontomesh.onerecord.client.service;

import io.ontomesh.core.enums.ProtocolType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

/**
 * @author Arun Kumar K
 * Process the incoming notification message and send it to One Record Client via REST endpoint of subscriber.
 */
@Slf4j
@Service
public class NotificationMessageProcessor {

    public static final String TO = "to";
    public static final String PROTOCOL = "protocol";
    private final INotificationClient notificationClient;

    public NotificationMessageProcessor(INotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    /**
     * Process the incoming notification message
     *
     * @param message Notification Message as String
     */
    public void process(Message<String> message) {
        MessageHeaders headers = message.getHeaders();

        ProtocolType protocolType = getProtocolType(headers);
        String to = getHeader(headers, TO);

        if (protocolType == ProtocolType.EMAIL) {
            notificationClient.sendNotification(to, message.getPayload());
        }

        Acknowledgment acknowledgment = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
        if (acknowledgment != null) {
            log.info("Acknowledgment provided");
            acknowledgment.acknowledge();
        }
    }

    /**
     * Returns the Subscribers Protocol Type
     *
     * @param headers Message Headers
     * @return ProtocolType
     */
    private static @Nullable ProtocolType getProtocolType(MessageHeaders headers) {
        String protocol = getHeader(headers, PROTOCOL);
        return ProtocolType.fromValue(protocol);
    }

    /**
     * returns the message headers as String.
     *
     * @param headers Message Headers
     * @param key     Message Header Key
     * @return Message Header Value
     */
    private static String getHeader(MessageHeaders headers, String key) {
        return (String.valueOf(headers.get(key)));
    }
}
