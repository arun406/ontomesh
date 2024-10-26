package io.ontomesh.onerecord.client.service;

import io.ontomesh.onerecord.client.OneRecordClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

@Slf4j
@Component
public class NotificationClient implements INotificationClient {

    private final Converter<Model, String> converter;

    public NotificationClient(
            @Qualifier("jsonLDToStringConverter") Converter<Model, String> converter, OneRecordClient oneRecordClient) {
        this.converter = converter;
        this.oneRecordClient = oneRecordClient;
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
     * @param notification
     */
    public void sendNotification(String url, Model notification) {
        if (notification.isEmpty()) {
            return;
        }

        String notificationMessage = getNotificationMessage(notification);
        log.debug("Notification message {}", notificationMessage);
        this.sendNotification(url, notificationMessage);
    }

    /**
     * Sends Notification
     *
     * @param url
     * @param notification
     */
    @Override
    public void sendNotification(String url, String notification) {
        if (!StringUtils.hasText(notification)) {
            return;
        }
//        WebClient webClient = OneRecordWebClient.createDefaultClient(url);
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(url);
        sendNotification(factory, notification);
    }

    private final OneRecordClient oneRecordClient;

    /**
     * Sends Notification.
     *
     * @param notification Notification.
     */
    private void sendNotification(UriBuilderFactory factory, String notification) {
//        OneRecordClientExceptionHandler errorHandler = new OneRecordClientExceptionHandler();
//        OneRecordClientSuccessHandler successHandler = new OneRecordClientSuccessHandler();
//        Mono<ResponseEntity<Void>> response = webClient
//                .post()
//                .uri(Constants.NOTIFICATION_URI)
//                .header(HttpHeaders.CONTENT_TYPE, "application/ld+json")
//                .body(BodyInserters.fromValue(notification))
//                .retrieve()
//                .onStatus(HttpStatusCode::isError, errorHandler::handleError)
//                .onStatus(HttpStatusCode::is2xxSuccessful, successHandler::handleSuccess)
//                .toBodilessEntity();
//
//        response.subscribe();

        oneRecordClient.sendNotification(factory, notification);
    }

}
