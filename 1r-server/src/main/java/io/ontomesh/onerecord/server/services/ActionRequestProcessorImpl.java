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
    private final SnapshotService snapshotService;

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

    private void processAccessDelegationRequest(LogisticsObject accessDelegationRequest) {

    }

    private void processSubscriptionRequest(LogisticsObject subscriptionRequest) {
//        add event for subscription.
//        API.SUBSCRIPTION_REQUEST_ACCEPTED,
//        API.SUBSCRIPTION_REQUEST_REVOKED,
//        API.SUBSCRIPTION_REQUEST_PENDING,
//        API.SUBSCRIPTION_REQUEST_REJECTED,
//        API.SUBSCRIPTION_REQUEST_FAILED,

        Model subscriptionRequestModel = subscriptionRequest.model();
        IRI status = Models.objectIRI(subscriptionRequestModel.getStatements(subscriptionRequest.iri(), API.hasRequestStatus, null))
                .orElse(null);
        log.debug("Action request status : {}", status);

        Model subscriptionModel = RDFUtil.getSubModel(subscriptionRequestModel, API.hasSubscription);
        IRI subscriptionIRI = RDFUtil.getRootIri(subscriptionModel);
        log.debug("Subscription IRI {} and Type {} ", subscriptionIRI, API.Subscription);
        LogisticsObject subscription = new LogisticsObject(subscriptionIRI, API.Subscription, subscriptionModel);
        IRI topicTypeIRI = Models.objectIRI(subscriptionModel.filter(subscriptionIRI, API.hasTopicType, null))
                .orElse(null);
        log.debug("Subscription Topic Type :{}", topicTypeIRI);
        if (topicTypeIRI == null) {
            return;
        }
        if (topicTypeIRI.equals(API.LOGISTICS_OBJECT_IDENTIFIER)) {
            this.subscriptionService.processSubscriptionRequest(subscription, status);
        }
    }

    private void processChangeRequest(LogisticsObject changeRequest) {

        Model changeModel = RDFUtil.getSubModel(changeRequest.model(), API.hasChange);
        IRI changeIRI = RDFUtil.getRootIri(changeModel);
//        IRI changeType = RDFUtil.getType(changeModel, changeIRI);
        log.debug("Change IRI {} and Type {} ", changeIRI, API.Change);

        LogisticsObject change = new LogisticsObject(changeIRI, API.Change, changeModel);
//        RDFUtil.print(change, RDFFormat.TURTLE);

        IRI logisticsObjectIRI = Models
                .objectIRI(changeModel.filter(changeIRI, API.hasLogisticsObject, null))
                .orElse(null);

        log.debug("Logistics Object IRI {}", logisticsObjectIRI);
        if (logisticsObjectIRI == null) {
            return;
        }
        LogisticsObject logisticsObject = logisticsObjectService.findLogisticsObject(logisticsObjectIRI);
        logisticsObjectService.processActionRequest(change, logisticsObject);
        LogisticsObject snapshot = snapshotService.createSnapshot(logisticsObject);
        log.debug("Snapshot with {} is created successfully.", snapshot.iri());
    }
}
