package io.ontomesh.onerecord.client.service;

import io.ontomesh.core.enums.ProtocolType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Processes incoming notification messages and sends them to One Record Client via the subscriber's REST endpoint.
 * Handles acknowledgment for Kafka messages.
 *
 * @author Arun Kumar K
 */
@Slf4j
@Service
public class NotificationMessageProcessor {

    private static final String TO = "to";
    private static final String PROTOCOL = "protocol";

    private final INotificationClient notificationClient;

    /**
     * Constructor for NotificationMessageProcessor.
     *
     * @param notificationClient The notification client to send messages.
     */
    public NotificationMessageProcessor(INotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    /**
     * Processes the incoming notification message.
     *
     * @param message Notification message containing headers and payload.
     */
    public void process(Message<String> message) {
        MessageHeaders headers = message.getHeaders();
        ProtocolType protocolType = getProtocolType(headers);
        String to = getHeader(headers, TO);

        // Validate protocol type and recipient
        if (protocolType != ProtocolType.HTTP || !StringUtils.hasText(to)) {
            log.warn("Invalid notification request - Protocol: {}, Recipient: '{}'. Notification will not be sent.", protocolType, to);
            acknowledgeMessage(headers);
            return;
        }

        // Send notification
        log.info("Sending notification to: {}", to);
        notificationClient.sendNotification(Values.iri(to), message.getPayload());

        // Acknowledge message
        acknowledgeMessage(headers);
    }


    /**
     * Retrieves the protocol type from message headers.
     *
     * @param headers Message headers
     * @return ProtocolType extracted from headers or {@code null} if not found
     */
    private static ProtocolType getProtocolType(MessageHeaders headers) {
        String protocol = getHeader(headers, PROTOCOL);
        if (protocol == null) {
            log.warn("Protocol type header is missing. Defaulting to null.");
            return null;
        }
        return ProtocolType.fromValue(protocol);
    }

    /**
     * Retrieves the specified header value as a string.
     *
     * @param headers Message headers
     * @param key     Header key
     * @return Header value as a string, or {@code null} if not found
     */
    private static String getHeader(MessageHeaders headers, String key) {
        Object value = headers.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Acknowledges the Kafka message if an acknowledgment header is present.
     *
     * @param headers Message headers
     */
    private void acknowledgeMessage(MessageHeaders headers) {
        Acknowledgment acknowledgment = headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
        if (acknowledgment == null) {
            log.warn("Acknowledgment not found in headers. Message may not be properly acknowledged.");
            return;
        }
        log.debug("Acknowledging message.");
        acknowledgment.acknowledge();
    }
}
