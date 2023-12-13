package io.ontomesh.onerecord.client.service;


import io.ontomesh.onerecord.client.OneRecordWebClient;
import io.ontomesh.onerecord.client.converter.ModelToStringConverter;
import io.ontomesh.onerecord.client.converter.StringToModelConverter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class SubscriptionClientService {

    private final OneRecordWebClient client;

    private final ModelToStringConverter modelToStringConverter;

    private final StringToModelConverter stringToModelConverter;

    public SubscriptionClientService(OneRecordWebClient client,
                                     @Qualifier("jsonLDToStringConverter") ModelToStringConverter modelToStringConverter,
                                     @Qualifier("stringToJsonLDConverter") StringToModelConverter stringToModelConverter) {
        this.client = client;
        this.modelToStringConverter = modelToStringConverter;
        this.stringToModelConverter = stringToModelConverter;
    }

    public Model initiateSubscription(IRI topic, IRI topicType) {
        Model subscriptionModel = new DynamicModelFactory().createEmptyModel();
        if (topic == null) {
            log.error("topic is mandatory to initiate subscription.");
            return null;
        }
        if (topicType == null) {
            log.error("topic type is mandatory to initiate subscription.");
            return null;
        }

        String subscription = this.client.initiateSubscription(topic, topicType);

        if (StringUtils.hasText(subscription)) {
            subscriptionModel = stringToModelConverter.convert(subscription);
            log.debug("subscription received : {}", subscriptionModel.subjects());
        }
        return subscriptionModel;
    }
}
