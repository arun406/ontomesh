package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.common.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.services.ActionRequestService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/action-requests")
public class ActionRequestController extends BaseController {

    /**
     * constructor.
     *
     * @param actionRequestService
     */
    public ActionRequestController(ActionRequestService actionRequestService) {
        super(actionRequestService);
    }

    /**
     * Get Action Request Details
     *
     * @param actionRequestReference action request reference.
     * @return Action Request.
     */
    @GetMapping(value = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> get(@PathVariable("id") String actionRequestReference) {

        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        log.debug("action request id {}", actionRequestId);

        LogisticsObject actionRequest = actionRequestService.get(actionRequestId.iri());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SemanticWebMediaType.APPLICATION_JSON_LD);
        headers.setContentLanguage(Locale.US);
        headers.add("Type", actionRequest.type().stringValue());
        headers.setLastModified(Instant.now()); //TODO

        return ResponseEntity.ok().headers(headers).body(actionRequest);
    }

    /**
     * Update an Action Request.
     *
     * <p>
     * This API action can be used the holder/publisher of a Logistics Object to approve or reject
     * a pending ActionRequest.
     *
     * @param actionRequestReference action request reference.
     * @param status                 status of the action request.
     * @return no content.
     */
    @PatchMapping(value = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> update(@PathVariable("id") String actionRequestReference,
                                       @RequestParam(required = false, name = "status") String status) {

        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        if (!StringUtils.hasText(status)) {
            throw new RuntimeException("status cannot be blank.");
        }
        IRI statusIRI = Values.iri(status);
        this.actionRequestService.updateStatus(actionRequestId.iri(), statusIRI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SemanticWebMediaType.APPLICATION_JSON_LD);
        headers.setLocation(actionRequestId.uri());

        return ResponseEntity.noContent().headers(headers).build();
    }

    /**
     * Revoke Action Request.
     *
     * <p>
     * This API action MUST be used to revoke an Action Request MUST be revoked only by the original requester of the
     * ActionRequest or the holder/publisher of the Logistics Object.
     *
     * @param actionRequestReference action request reference.
     * @return no content.
     */
    @DeleteMapping(value = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> delete(@PathVariable("id") String actionRequestReference) {

        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        log.debug("deleting action request of id {} ", actionRequestId.iri());

        // A ChangeRequest MUST only be revoked as long as it is in REQUEST_PENDING status
        this.actionRequestService.revokeActionRequest(actionRequestId.iri(), idProvider.createUniqueLoUri().iri());
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.noContent().headers(headers).build();
    }
}
