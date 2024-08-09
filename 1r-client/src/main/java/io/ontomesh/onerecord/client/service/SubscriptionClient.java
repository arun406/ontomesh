package io.ontomesh.onerecord.client.service;


import io.ontomesh.onerecord.client.api.Constants;
import io.ontomesh.onerecord.client.handlers.OneRecordClientExceptionHandler;
import io.ontomesh.onerecord.client.http.OneRecordWebClient;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class SubscriptionClient implements ISubscriptionClient {

    private final Converter<String, Model> stringToModelConverter;

    public SubscriptionClient(@Qualifier("stringToJsonLDConverter") Converter<String, Model> stringToModelConverter) {
        this.stringToModelConverter = stringToModelConverter;
    }

    /**
     * Initiate Subscription.
     *
     * @param topic
     * @param topicType
     * @return
     */
    public Model initiateSubscription(String url, IRI topic, IRI topicType) {

        Model subscriptionModel = new DynamicModelFactory().createEmptyModel();
        if (topic == null) {
            log.error("Topic is mandatory to initiate subscription.");
            return null;
        }
        if (topicType == null) {
            log.error("Topic type is mandatory to initiate subscription.");
            return null;
        }
        WebClient webClient = OneRecordWebClient.createDefaultClient(url);
        String subscription = initiateSubscription(webClient, topic, topicType);
        if (StringUtils.hasText(subscription)) {
            subscriptionModel = stringToModelConverter.convert(subscription);

            log.debug("Subscription received : {}", subscriptionModel.subjects());
        }
        return subscriptionModel;
    }

    /**
     * Initiate Subscription.
     *
     * @param webClient
     * @param topic
     * @param topicType
     * @return
     */
    private String initiateSubscription(WebClient webClient, IRI topic, IRI topicType) {

        OneRecordClientExceptionHandler exceptionHandler = new OneRecordClientExceptionHandler();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.SUBSCRIPTION_URI)
                        .queryParam(Constants.QUERY_PARAM_TOPIC, topic)
                        .queryParam(Constants.QUERY_PARAM_TOPIC_TYPE, topicType)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, exceptionHandler::handleError)
                .bodyToMono(String.class)
                .block();
    }
}
