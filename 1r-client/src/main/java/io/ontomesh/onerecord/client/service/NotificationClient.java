package io.ontomesh.onerecord.client.service;

import com.innovasolutions.framework.webclient.RegisteredWebClient;
import com.innovasolutions.framework.webclient.RegisteredWebClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class NotificationClient implements INotificationClient {

    @Autowired
    private RegisteredWebClientRepository repository;

    private final Converter<Model, String> converter;

    public NotificationClient(@Qualifier("jsonLDToStringConverter") Converter<Model, String> converter) {
        this.converter = converter;
//        this.oneRecordClient = oneRecordClient;
    }

    /*    *//**
     * Convert Model To String.
     *
     * @param notificationModel notification model
     * @return rdf string
     *//*
    private String getNotificationMessage(Model notificationModel) {
        return converter.convert(notificationModel);
    }*/

    /*    *//**
     * Sends Notification.
     *
     * @param url
     * @param notification
     *//*
    public void sendNotification(String url, Model notification) {
        if (notification.isEmpty()) {
            return;
        }

        String notificationMessage = getNotificationMessage(notification);
        log.debug("Notification message {}", notificationMessage);
        this.sendNotification(url, notificationMessage);
    }*/

    /*    */
    /*
     */

    /**
     * Sends Notification
     *
     * @param url
     * @param notification
     *//*
    @Override
    public void sendNotification(IRI url, String notification) {
        if (!StringUtils.hasText(notification)) {
            return;
        }
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(url);
        sendNotification(factory, notification);
    }*/
    public void sendNotification(IRI clientId, String notification) {
        if (!StringUtils.hasText(notification)) {
            return;
        }

        // Query for a client and service by client ID and service key
        RegisteredWebClient registeredWebClient = repository.findByClientAndServiceId(clientId.stringValue(), "notifications");

        // Validate that the client-service pair exists
        if (registeredWebClient == null) {
            log.info("Client or Service not found!");
            return;
        }

        Mono<Void> response = null;
        try {
            response = registeredWebClient.execute(notification);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        response.block();
    }

    /*    private final OneRecordClient oneRecordClient;

     *//**
     * Sends Notification.
     *
     * @param notification Notification.
     *//*
    private void sendNotification(UriBuilderFactory factory, String notification) {
        oneRecordClient.sendNotification(factory, notification);
    }*/

}
