package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import com.ontomesh.onerecord.server.util.RDFUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionRequestService {

    private final IdProvider idProvider;
    private final LogisticsObjectRepository repository;
    private final ActionRequestProcessor actionRequestProcessor;

    /**
     * create action request
     *
     * @param request logistic object action request
     * @param type    action request type.
     */
    @Transactional
    public IRI createActionRequest(LogisticsObject request, IRI type) {

        validateActionRequestType(type);
        Model requestModel = request.model();
        IRI actionRequestID = idProvider.createUniqueActionRequestUri().iri();
        IRI requestedBy = idProvider.createUniqueLoUri().iri();
        Model actionRequestModel = new ModelBuilder()
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(actionRequestID)
                .add(RDF.TYPE, type)
                .add(API.isRequestedAt, Date.from(Instant.now()))
                .add(API.hasRequestStatus, API.REQUEST_PENDING)
                .add(API.isRequestedBy, requestedBy)
                .subject(request.iri())
                .build();

        if (API.SubscriptionRequest.stringValue().equals(type.stringValue())) {
            actionRequestModel.add(actionRequestID, API.hasSubscription, request.iri());
        } else if (API.ChangeRequest.stringValue().equals(type.stringValue())) {
            actionRequestModel.add(actionRequestID, API.hasChange, request.iri());
        } else if (API.AccessDelegationRequest.stringValue().equals(type.stringValue())) {
            actionRequestModel.add(actionRequestID, API.hasAccessDelegation, request.iri());
        }
        actionRequestModel.addAll(requestModel);
        this.repository.persist(actionRequestModel);
        return actionRequestID;
    }

    private void validateActionRequestType(IRI type) {
        if (!Arrays.asList(API.ChangeRequest.stringValue(),
                API.SubscriptionRequest.stringValue(),
                API.AccessDelegationRequest.stringValue()).contains(type.stringValue())) {
            throw new RuntimeException(String.format("Invalid Action Request Type %s", type.stringValue()));
        }
    }

    public LogisticsObject get(IRI iri) {
        return this.repository.findLoById(iri, (iri1) -> true)
                .orElseThrow(() -> new LogisticsObjectNotFoundException(iri, "action request with iri " + iri + " not found."));
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
        IRI status = Models.objectIRI(model.getStatements(iri, API.hasRequestStatus, null))
                .orElse(null);
        if (status == null || !status.stringValue().equals(API.REQUEST_PENDING.stringValue())) {
            throw new RuntimeException("Cannot revoke the action request as currentStatus is not pending.");
        }
        model.add(iri, API.isRevokedBy, revokedBy);
        model.add(iri, API.isRevokedAt, Values.literal(Date.from(Instant.now())));
        IRI revokeStatus = getRevokeRequestStatus(iri, model);
        updateStatus(iri, status, revokeStatus);
    }

    /**
     * update the status of the action request.
     *
     * @param iri           action request iri.
     * @param currentStatus current status iri.
     * @param newStatus     new status iri.
     */
    private void updateStatus(IRI iri, IRI currentStatus, IRI newStatus) {
        this.repository.delete(iri, API.hasRequestStatus, currentStatus);
        log.debug("updating the request status from {} to new request status {}", currentStatus, newStatus);
        this.repository.add(iri, API.hasRequestStatus, newStatus);
    }

    private IRI getRevokeRequestStatus(IRI iri, Model model) {
        IRI actionRequestType = RDFUtil.getType(model, iri);
        if (actionRequestType.stringValue().equals(API.ChangeRequest.stringValue())) {
            return API.CHANGE_REQUEST_REVOKED;
        } else if (actionRequestType.stringValue().equals(API.SubscriptionRequest.stringValue())) {
            return API.SUBSCRIPTION_REQUEST_REVOKED;
        } else if (actionRequestType.stringValue().equals(API.AccessDelegationRequest.stringValue()))
            return API.ACCESS_DELEGATION_REQUEST_REVOKED;
        return API.REQUEST_PENDING;
    }

    /**
     * update the status to new value.
     *
     * @param iri       action request iri.
     * @param newStatus new action request status.
     */
    @Transactional
    public void updateStatus(IRI iri, IRI newStatus) {

        LogisticsObject actionRequest = this.get(iri);
        Model model = actionRequest.model();
        IRI status = Models.objectIRI(model.getStatements(iri, API.hasRequestStatus, null))
                .orElse(null);
        log.debug("current action request status {}", status);
        if (status == null || newStatus.stringValue().equals(status.stringValue())) {
            throw new RuntimeException(String.format("Cannot update status. Action Request status is already %s)", newStatus));
        }
        updateStatus(iri, status, newStatus);

        actionRequestProcessor.process(actionRequest);

    }
}
