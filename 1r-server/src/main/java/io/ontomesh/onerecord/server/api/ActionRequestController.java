package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import io.ontomesh.onerecord.server.util.URIUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/action-requests")
public class ActionRequestController extends BaseController {

    public static final List<IRI> VALID_ACTION_REQUEST_STATUSES = Arrays.asList(API.REQUEST_ACCEPTED,
            API.REQUEST_PENDING,
            API.REQUEST_FAILED,
            API.REQUEST_REJECTED,
            API.REQUEST_REVOKED);

    /**
     * constructor.
     *
     * @param actionRequestService
     */
    public ActionRequestController(ActionRequestService actionRequestService) {
        super(actionRequestService);
    }


    @GetMapping(value = "", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<List<ActionRequest>> listAll(@RequestParam(value = "status", required = false) String status,
                                                       @RequestParam(value = "page", defaultValue = "0") int offset,
                                                       @RequestParam(value = "size", defaultValue = "20") int limit) {

        List<ActionRequest> actionRequests = this.actionRequestService
                .listAll(API.SubscriptionRequest, limit, offset, StringUtils.hasText(status) ? Values.iri(status) : null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SemanticWebMediaType.APPLICATION_JSON);
        headers.setContentLanguage(Locale.US);
        headers.setLastModified(Instant.now());

        return ResponseEntity.ok().headers(headers).body(actionRequests);
    }

    /**
     * Get Action Request Details
     *
     * @return Action Request.
     */
    @GetMapping(value = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> get() {

        UriComponents uriComponents = ServletUriComponentsBuilder.fromCurrentRequest().build();
        URI arURI = uriComponents.toUri();
        log.debug("action request id {}", arURI);
        IRI arIRI = URIUtil.toIRI(arURI);

        LogisticsObject actionRequest = actionRequestService.get(arIRI);

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

        if (!StringUtils.hasText(status)) {
            throw new RuntimeException("status cannot be blank.");
        }
        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        IRI newStatus = null;

        try {
            newStatus = Values.iri(status);
        } catch (IllegalArgumentException iae) {
            if (!VALID_ACTION_REQUEST_STATUSES.contains(Values.iri(API.NAMESPACE + status))) {
                throw new RuntimeException("Invalid status.");
            }
            newStatus = Values.iri(API.NAMESPACE + status);
        }
        this.actionRequestService.updateStatus(actionRequestId.iri(), newStatus);
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
