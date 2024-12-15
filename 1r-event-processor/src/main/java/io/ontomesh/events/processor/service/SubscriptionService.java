package io.ontomesh.events.processor.service;

import io.ontomesh.events.processor.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service class for managing subscriptions.
 */
@Slf4j
@Service
public class SubscriptionService {

    public static final IRI INCLUDE_SUBSCRIPTION_EVENT_TYPE = Values.iri("https://onerecord.iata.org/ns/api#includeSubscriptionEventType");
    private final SubscriptionRepository repository;

    public SubscriptionService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * List all subscribers for the given loId, loType and eventType.
     *
     * @param loId      the logistics object id
     * @param loType    the logistics object type
     * @param eventType the event type
     * @return the map of subscribers
     */
    public Map<IRI, Model> listSubscribers(IRI loId, IRI loType, IRI eventType) {
        Model model = repository.listAllSubscribers(loId, loType);
        Set<IRI> iris = Models.subjectIRIs(model);
        Map<IRI, Model> subscribersMap = new HashMap<>();
        for (IRI iri : iris) {
            Model statements = model.filter(iri, null, null);
            Model eventTypeStatements = model.filter(iri, INCLUDE_SUBSCRIPTION_EVENT_TYPE, eventType);
            if (eventTypeStatements != null) {
                subscribersMap.put(iri, statements);
            }
        }
        log.info("No of subscribers available for the event {} of lo id {} of type {} is {}",
                eventType, loId, loType, subscribersMap.size());

        return subscribersMap;
    }
//    public Map<IRI, Model> listSubscribers(IRI loId, IRI loType, IRI eventType) {
//        Model model = repository.listAllSubscribers(loId, loType);
//        Map<IRI, Model> subscribersMap = Models.subjectIRIs(model).stream()
//                .filter(iri -> model.contains(iri, INCLUDE_SUBSCRIPTION_EVENT_TYPE, eventType))
//                .collect(Collectors.toMap(iri -> iri, iri -> model.filter(iri, null, null)));
//
//        log.info("Number of subscribers for event {} of loId {} and type {}: {}",
//                eventType, loId, loType, subscribersMap.size());
//
//        return subscribersMap;
//    }

}
