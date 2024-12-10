package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SubscriptionRequestProcessorImpl implements ActionRequestProcessor {

    private final SubscriptionService subscriptionService;

    public SubscriptionRequestProcessorImpl(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void process(LogisticsObject subscriptionRequest) {
        Model model = subscriptionRequest.model();
        IRI status = getStatus(model, subscriptionRequest.iri());
        log.debug("Action request status : {}", status);

        Model subscriptionModel = RDFUtil.getSubModel(model, API.hasSubscription);
        IRI subscriptionIRI = RDFUtil.getRootIri(subscriptionModel);
        log.debug("Subscription IRI {} and Type {} ", subscriptionIRI, API.Subscription);
        LogisticsObject subscription = new LogisticsObject(subscriptionIRI, API.Subscription, subscriptionModel);
        IRI topicTypeIRI = getTopicTypeIRI(subscriptionModel, subscriptionIRI);
        if (topicTypeIRI == null) {
            log.warn("Subscription Topic Type is null");
            return;
        }
        if (topicTypeIRI.equals(API.LOGISTICS_OBJECT_IDENTIFIER)) {
            this.subscriptionService.processSubscriptionRequest(subscription, status);
        }
    }

    @Override
    public IRI requestType() {
        return API.SubscriptionRequest;
    }

    private IRI getStatus(Model model, IRI iri) {
        return Models.objectIRI(model.filter(iri, API.hasRequestStatus, null))
                .orElse(null);
    }

    private IRI getTopicTypeIRI(Model model, IRI iri) {
        return Models.objectIRI(model.filter(iri, API.hasTopicType, null))
                .orElse(null);
    }

}
