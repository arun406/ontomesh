package io.ontomesh.onerecord.client.service;

import com.innovasolutions.framework.webclient.RegisteredWebClient;
import com.innovasolutions.framework.webclient.RegisteredWebClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class NotificationClient implements INotificationClient {

    private final RegisteredWebClientRepository repository;
    private final Converter<Model, String> converter;

    /**
     * Constructor for NotificationClient.
     *
     * @param repository RegisteredWebClientRepository for fetching registered clients
     * @param converter  Converter for transforming RDF Model into a String
     */
    public NotificationClient(RegisteredWebClientRepository repository,
                              @Qualifier("jsonLDToStringConverter") Converter<Model, String> converter) {
        this.repository = repository;
        this.converter = converter;
    }

    /**
     * Sends a notification message.
     *
     * @param clientId     The client identifier as an IRI
     * @param notification The notification message in string format
     */
    @Override
    public void sendNotification(IRI clientId, String notification) {
        if (!StringUtils.hasText(notification)) {
            log.warn("Notification content is empty. Skipping sending.");
            return;
        }

        // Fetch the registered client for notifications
        RegisteredWebClient registeredWebClient = repository.findByClientAndServiceId(clientId.stringValue(), "notifications");

        if (registeredWebClient == null) {
            log.warn("No registered client found for ID: {}", clientId);
            return;
        }

        try {
            Mono<Void> response = registeredWebClient.execute(notification);
            response.block();
            log.info("Notification sent successfully to client: {}", clientId);
        } catch (ClassNotFoundException e) {
            log.error("Error while sending notification to client: {}", clientId, e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    /**
     * Converts an RDF Model into a notification message string.
     *
     * @param notificationModel The RDF model representing the notification
     * @return The notification as a string
     */
    private String convertNotificationModel(Model notificationModel) {
        return converter.convert(notificationModel);
    }
}
