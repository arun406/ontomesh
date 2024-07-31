package io.ontomesh.events.processor.service;

import io.ontomesh.events.processor.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class SubscriptionService {

    public static final IRI INCLUDE_SUBSCRIPTION_EVENT_TYPE = Values.iri("https://onerecord.iata.org/ns/api#includeSubscriptionEventType");
    private final SubscriptionRepository repository;

    public SubscriptionService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * @param loId
     * @param loType
     * @param eventType
     * @return
     */
    public Map<IRI, Model> listSubscribers(IRI loId, IRI loType, IRI eventType) {
        Model model = repository.listAllSubscribers(loId, loType);
        Set<IRI> iris = Models.subjectIRIs(model);
        Map<IRI, Model> subscribersMap = new HashMap<>();
        for (IRI iri : iris) {
            Model statements = model.filter(iri, null, null);
            Model eventTypeStatements = model
                    .filter(iri, INCLUDE_SUBSCRIPTION_EVENT_TYPE, eventType);

            if (eventTypeStatements != null) {
                subscribersMap.put(iri, statements);
            }
        }
        log.info("No of subscribers available for the event {} of lo id {} of type {} is {}",
                eventType, loId, loType, subscribersMap.size());

        return subscribersMap;
    }
}
