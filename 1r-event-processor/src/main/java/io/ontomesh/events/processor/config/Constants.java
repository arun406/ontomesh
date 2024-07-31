package io.ontomesh.events.processor.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

public interface Constants {
    IRI EVENT_FOR = Values.iri("https://onerecord.iata.org/ns/cargo#eventFor");
    IRI EVENT_CODE = Values.iri("https://onerecord.iata.org/ns/cargo#eventCode");
    IRI LOGISTICS_EVENT = Values.iri("https://onerecord.iata.org/ns/cargo#LogisticsEvent");
    IRI SUBSCRIPTION_REQUEST = Values.iri("https://onerecord.iata.org/ns/api#SubscriptionRequest");
    IRI TOPIC = Values.iri("https://onerecord.iata.org/ns/api#hasTopic");
}
