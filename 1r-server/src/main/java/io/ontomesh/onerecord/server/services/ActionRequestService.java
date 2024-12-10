package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.api.enums.RequestType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.iata.onerecord.api.API.SubscriptionRequest;

@Slf4j
@Service
public class ActionRequestService extends AbstractLogisticsObjectService {

    /**
     * Logistics Event Service.
     */
    private final LogisticsEventService logisticsEventService;

    /**
     * Action Request Processor.
     */
    private final ActionRequestProcessExecutor actionRequestProcessExecutor;

    /**
     * @param repository repository
     * @param idProvider id Provider
     */
    protected ActionRequestService(LogisticsObjectRepository repository, IdProvider idProvider, LogisticsEventService logisticsEventService,
                                   ActionRequestProcessExecutor actionRequestProcessExecutor) {
        super(repository, idProvider);
        this.logisticsEventService = logisticsEventService;
        this.actionRequestProcessExecutor = actionRequestProcessExecutor;
    }

    private static IRI validateAndGetActionRequestStatus(IRI iri, IRI newStatus, Model model) {
        IRI status = getStatus(iri, model);
        log.debug("current action request status {}", status);
        if (isSameStatus(newStatus, status) || isAlreadyAccepted(status)) {
            throw new InvalidApiRequestException(String.format("Cannot update status. Action Request status is already %s)",
                    newStatus));
        }
        return status;
    }

    private static boolean isSameStatus(IRI newStatus, IRI status) {
        if (newStatus == null || status == null)
            return true;
        return newStatus.equals(status);
    }

    private static boolean isAlreadyAccepted(IRI status) {
        if (status == null) {
            return true;
        }
        return status.equals(API.REQUEST_ACCEPTED);
    }

    private static IRI getStatus(IRI iri, Model model) {
        return RDFUtil.getObjectIRI(model, iri, API.hasRequestStatus, false);
    }

    private void addEventToLogisticsObject(Model model, IRI iri, IRI type, IRI status) {
        IRI logisticsObjectIRI;
        NotificationEventType notificationEventType;

        if (!type.equals(SubscriptionRequest)) {
            logisticsObjectIRI = getLogisticsObjetIRI(model, iri);
            if (type.equals(API.ChangeRequest)) {
                notificationEventType = LogisticsObjectService.getNotificationEventType(status);
            } else {
                notificationEventType = null;
            }
        } else {
            Model subscriptionModel = RDFUtil.getSubModel(model, API.hasSubscription);
            IRI subscriptionIRI = RDFUtil.getRootIri(subscriptionModel);
            logisticsObjectIRI = SubscriptionService.getTopicIRI(subscriptionModel, subscriptionIRI);
            notificationEventType = SubscriptionService.getNotificationEventType(status);
        }
        if (logisticsObjectIRI == null) {
            return;
        }

        assert notificationEventType != null;
        this.logisticsEventService.addEventToLogisticsObject(logisticsObjectIRI, notificationEventType);
    }

    private IRI getLogisticsObjetIRI(Model model, IRI iri) {
        return RDFUtil.getObjectIRI(model, iri, API.hasLogisticsObject, false);
    }

    private IRI getRevokeRequestStatus(IRI iri, Model model) {
        IRI actionRequestType = RDFUtil.getType(model, iri);
        if (actionRequestType.stringValue().equals(API.ChangeRequest.stringValue())) {
            return API.CHANGE_REQUEST_REVOKED;
        } else if (actionRequestType.stringValue().equals(SubscriptionRequest.stringValue())) {
            return API.SUBSCRIPTION_REQUEST_REVOKED;
        } else if (actionRequestType.stringValue().equals(API.AccessDelegationRequest.stringValue()))
            return API.ACCESS_DELEGATION_REQUEST_REVOKED;
        return API.REQUEST_PENDING;
    }

    private void saveActionRequestModel(Model actionRequestModel) {
        this.getRepository().persist(actionRequestModel);
    }

    /**
     * update the status of the action request.
     *
     * @param iri           action request iri.
     * @param currentStatus current status iri.
     * @param newStatus     new status iri.
     */
    private void updateStatus(IRI iri, IRI currentStatus, IRI newStatus) {
        this.getRepository().update(iri, API.hasRequestStatus, currentStatus, newStatus);
    }

    private void validateActionRequestType(IRI type) {
        if (!Arrays.asList(API.ChangeRequest.stringValue(),
                SubscriptionRequest.stringValue(),
                API.AccessDelegationRequest.stringValue()).contains(type.stringValue())) {
            throw new RuntimeException(String.format("Invalid Action Request Type %s", type.stringValue()));
        }
    }

    /**
     * create action request
     *
     * @param request logistic object action request
     * @param type    action request type.
     */
    @Transactional
    public IRI createActionRequest(LogisticsObject request, RequestType type) {
//        validateActionRequestType(type);
        IRI actionRequestID = getIdProvider().createUniqueActionRequestUri().iri();
        IRI requestedBy = getIdProvider().createUniqueLoUri().iri();

        Model actionRequestModel = new ModelBuilder()
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(actionRequestID)
                .add(RDF.TYPE, type.getIri())
                .add(API.isRequestedAt, Date.from(Instant.now()))
                .add(API.hasRequestStatus, API.REQUEST_PENDING)
                .add(API.isRequestedBy, requestedBy)
                .subject(request.iri())
                .build();


        switch (type) {
            case SubscriptionRequest -> actionRequestModel.add(actionRequestID, API.hasSubscription, request.iri());
            case ChangeRequest -> actionRequestModel.add(actionRequestID, API.hasChange, request.iri());
            case AccessDelegationRequest ->
                    actionRequestModel.add(actionRequestID, API.hasAccessDelegation, request.iri());
            default -> {
                // Optional: Handle the case where none of the cases match
                log.warn("Unknown request type: {}", type);
                throw new IllegalArgumentException(String.format("Unsupported request type:%s ", type));
            }
        }

        actionRequestModel.addAll(request.model());

        saveActionRequestModel(actionRequestModel);

        return actionRequestID;
    }

    /**
     * @param iri Logistics Object IRI
     * @return Logistics Object
     */
    public LogisticsObject get(IRI iri) {

        Predicate<Statement> predicate = statement -> !(statement.getPredicate().equals(API.hasLogisticsObject)
                || statement.getPredicate().equals(API.hasTopic));
        return this.getRepository()
                .findLoById(iri, predicate)
                .orElseThrow(() -> new LogisticsObjectNotFoundException(iri, String.format("Action request with iri  %s not found.", iri)));
    }

    /**
     * revokes the action request.
     *
     * @param iri       action request iri.
     * @param revokedBy action request revoked by.
     */
    @Transactional
    public void revokeActionRequest(IRI iri, IRI revokedBy) {
        LogisticsObject actionRequest = this.get(iri);

        Model model = actionRequest.model();
        IRI status = getStatus(iri, model);
        if (status == null || !status.stringValue().equals(API.REQUEST_PENDING.stringValue())) {
            throw new RuntimeException("Cannot revoke the action request as currentStatus is not pending.");
        }
        model.add(iri, API.isRevokedBy, revokedBy);
        model.add(iri, API.isRevokedAt, Values.literal(Date.from(Instant.now())));
        IRI revokeStatus = getRevokeRequestStatus(iri, model);
        updateStatus(iri, status, revokeStatus);

        // send notification
        addEventToLogisticsObject(model, iri, revokeStatus);
    }

    public void addEventToLogisticsObject(Model model, IRI iri, IRI status) {
        log.debug("Status {}", status);
        IRI logisticsObjectIRI = getLogisticsObjetIRI(model, iri);

        log.info("Logistics Object IRI {}", logisticsObjectIRI);
        if (logisticsObjectIRI == null) {
            return;
        }
        logisticsEventService.addEventToLogisticsObject(logisticsObjectIRI, NotificationEventType.valueOf(status));
    }

    /**
     * Update the status to new value.
     *
     * @param iri       action request iri.
     * @param newStatus new action request status.
     */
    @Transactional
    public void updateStatus(IRI iri, IRI newStatus) {

        LogisticsObject actionRequest = this.get(iri);
        Model model = actionRequest.model();
        IRI currentStatus = validateAndGetActionRequestStatus(iri, newStatus, model);

        updateStatus(iri, currentStatus, newStatus);
        // update the currentStatus.
        actionRequest.add(API.hasRequestStatus, newStatus);
//        model.add(iri, API.hasRequestStatus, newStatus);

        this.actionRequestProcessExecutor.execute(actionRequest);

        // add Event
        addEventToLogisticsObject(model, iri, actionRequest.type(), newStatus);
    }

    /**
     * Returns action request by OntoMeshID.
     *
     * @param id ontomesh id
     * @return logistics object
     */
    public LogisticsObject get(OntoMeshId id) {
        if (id.iri() != null) {
            return get(id.iri());
        }
        return null;
    }

    public List<ActionRequest> listAll(IRI type, int limit, int offset, IRI status) {
        return this.getRepository().listLogisticsObject(type, limit, offset, status);
    }
}
