package io.ontomesh.onerecord.client.service;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public interface ISubscriptionClient {

    Model initiateSubscription(String url, IRI topic, IRI topicType);
}
