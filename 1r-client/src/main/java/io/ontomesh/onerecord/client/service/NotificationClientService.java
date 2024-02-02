package io.ontomesh.onerecord.client.service;

import io.ontomesh.onerecord.client.OneRecordWebClient;
import io.ontomesh.onerecord.client.converter.ModelToStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationClientService {

    private final OneRecordWebClient oneRecordWebClient;

    private final ModelToStringConverter converter;

    public NotificationClientService(OneRecordWebClient oneRecordWebClient,
                                     @Qualifier("jsonLDToStringConverter") ModelToStringConverter converter) {
        this.oneRecordWebClient = oneRecordWebClient;
        this.converter = converter;
    }

    /**
     * Convert Model To String.
     *
     * @param notificationModel notification model
     * @return rdf string
     */
    private String getNotificationMessage(Model notificationModel) {
        return converter.convert(notificationModel);
    }

    /**
     * Sends Notification.
     *
     * @param notificationModel
     */
    public void sendNotification(Model notificationModel) {
        if (notificationModel.isEmpty()) {
            log.debug("nothing to send..exiting");
            return;
        }

        String notificationMessage = getNotificationMessage(notificationModel);
        log.debug("Notification message {}", notificationMessage);
        this.oneRecordWebClient.sendNotification(notificationMessage);
    }
}
