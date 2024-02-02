package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

/**
 * @author arun
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ActionRequestProcessorImpl implements ActionRequestProcessor {

    private final LogisticsObjectService logisticsObjectService;
    private final SubscriptionService subscriptionService;

    private static LogisticsObject createChange(Model model) {
        Model changeModel = RDFUtil.getSubModel(model, API.hasChange);
        IRI changeIRI = RDFUtil.getRootIri(changeModel);
        log.debug("Change IRI {} and Type {} ", changeIRI, API.Change);
        return new LogisticsObject(changeIRI, API.Change, changeModel);
    }

    private LogisticsObject getLogisticsObject(Model model, IRI iri) {

        IRI logisticsObjectIRI = getLogisticsObjectIRI(model, iri);
        log.info("Logistics Object IRI {}", logisticsObjectIRI);
        if (logisticsObjectIRI == null) {
            return null;
        }
        return logisticsObjectService.findLogisticsObject(logisticsObjectIRI);
    }

    private IRI getLogisticsObjectIRI(Model model, IRI iri) {
        return Models.objectIRI(model.filter(iri, API.hasLogisticsObject, null))
                .orElse(null);
    }

    private IRI getStatus(Model model, IRI iri) {
        return Models.objectIRI(model.filter(iri, API.hasRequestStatus, null))
                .orElse(null);
    }

    private IRI getTopicTypeIRI(Model model, IRI iri) {
        return Models.objectIRI(model.filter(iri, API.hasTopicType, null))
                .orElse(null);
    }

    private void processAccessDelegationRequest(LogisticsObject accessDelegationRequest) {

    }

    private void processChangeRequest(LogisticsObject changeRequest) {

        LogisticsObject change = createChange(changeRequest.model());
        LogisticsObject logisticsObject = getLogisticsObject(change.model(), change.iri());
        if (logisticsObject == null) {
            log.warn("Change request {} do not contain logisticsObject reference.", changeRequest.iri());
            return;
        }

        logisticsObjectService.processChangeRequest(change, logisticsObject);
    }

    private void processSubscriptionRequest(LogisticsObject subscriptionRequest) {
//        add event for subscription.
//        API.SUBSCRIPTION_REQUEST_ACCEPTED,
//        API.SUBSCRIPTION_REQUEST_REVOKED,
//        API.SUBSCRIPTION_REQUEST_PENDING,
//        API.SUBSCRIPTION_REQUEST_REJECTED,
//        API.SUBSCRIPTION_REQUEST_FAILED,

        Model model = subscriptionRequest.model();
        IRI status = getStatus(model, subscriptionRequest.iri());
        log.debug("Action request status : {}", status);

        Model subscriptionModel = RDFUtil.getSubModel(model, API.hasSubscription);
        IRI subscriptionIRI = RDFUtil.getRootIri(subscriptionModel);
        log.debug("Subscription IRI {} and Type {} ", subscriptionIRI, API.Subscription);
        LogisticsObject subscription = new LogisticsObject(subscriptionIRI, API.Subscription, subscriptionModel);
        IRI topicTypeIRI = getTopicTypeIRI(subscriptionModel, subscriptionIRI);
        if (topicTypeIRI == null) {
            log.warn("Subscription Topic Type :{}", topicTypeIRI);
            return;
        }
        if (topicTypeIRI.equals(API.LOGISTICS_OBJECT_IDENTIFIER)) {
            this.subscriptionService.processSubscriptionRequest(subscription, status);
        }
    }

    /**
     * process the action request.
     *
     * @param actionRequest Action Request.
     */
    @Override
    public void process(LogisticsObject actionRequest) {
        log.debug("Processing action request");
        if (actionRequest.type().equals(API.ChangeRequest)) {
            processChangeRequest(actionRequest);
        } else if (actionRequest.type().equals(API.SubscriptionRequest)) {
            processSubscriptionRequest(actionRequest);
        } else if (actionRequest.type().equals(API.AccessDelegationRequest)) {
            processAccessDelegationRequest(actionRequest);
        }
    }
}
