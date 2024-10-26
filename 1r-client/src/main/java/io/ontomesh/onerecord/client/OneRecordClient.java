package io.ontomesh.onerecord.client;

import io.ontomesh.onerecord.client.api.Constants;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.util.UriBuilderFactory;

public interface OneRecordClient {

    @PostExchange(Constants.NOTIFICATION_URI)
    void sendNotification(UriBuilderFactory factory, @RequestBody String notification);

    @GetExchange(Constants.SUBSCRIPTION_URI)
    String initiateSubscription(UriBuilderFactory factory, @RequestParam("topic") IRI topic, @RequestParam("topicType") IRI topicType);

}
