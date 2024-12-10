package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import io.ontomesh.onerecord.server.domain.HttpHeadersBuilder;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_TURTLE_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.TEXT_TURTLE_VALUE;

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


    @GetMapping(produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<List<ActionRequest>>
    listAll(
            @RequestParam(value = "type", required = false, defaultValue = "https://onerecord.iata.org/ns/api#ChangeRequest") String type,
            @RequestParam(value = "status", required = false, defaultValue = "https://onerecord.iata.org/ns/api#REQUEST_PENDING") String status,
            @RequestParam(value = "page", defaultValue = "0") int offset,
            @RequestParam(value = "size", defaultValue = "20") int limit) {

        List<ActionRequest> actionRequests = this.actionRequestService
                .listAll(SimpleValueFactory.getInstance().createIRI(type), limit, offset, SimpleValueFactory.getInstance().createIRI(status));

        HttpHeaders headers = new HttpHeadersBuilder()
                .contentType(APPLICATION_JSON)
                .contentLanguage(Locale.US)
                .lastModified(Instant.now()).build();

        return ResponseEntity.ok().headers(headers).body(actionRequests);
    }

    /**
     * Get Action Request Details
     *
     * @return Action Request.
     */
    @GetMapping(value = "/{id}", produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> get(@PathVariable("id") String actionRequestReference) {

        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        LogisticsObject actionRequest = actionRequestService.get(actionRequestId.iri());

        HttpHeaders headers = new HttpHeadersBuilder()
                .contentType(APPLICATION_JSON_LD)
                .contentLanguage(Locale.US)
                .type(actionRequest.type().stringValue())
                .lastModified(Instant.now()).build();

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
    @PatchMapping(value = "/{id}",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> update(@PathVariable("id") String actionRequestReference,
                                       @RequestParam(required = false, name = "status") String status) {

        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("status cannot be blank.");
        }
        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        IRI newStatus = null;

        try {
            newStatus = Values.iri(status);
        } catch (IllegalArgumentException iae) {
            if (!VALID_ACTION_REQUEST_STATUSES.contains(Values.iri(API.NAMESPACE + status))) {
                throw new IllegalArgumentException("Invalid status.");
            }
            newStatus = Values.iri(API.NAMESPACE + status);
        }

        this.actionRequestService.updateStatus(actionRequestId.iri(), newStatus);
        HttpHeaders headers = new HttpHeadersBuilder()
                .location(actionRequestId.uri())
                .contentType(APPLICATION_JSON_LD).build();

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
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
    @DeleteMapping(value = "/{id}", produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> delete(@PathVariable("id") String actionRequestReference) {

        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        log.debug("deleting action request of id {} ", actionRequestId.iri());

        // A ChangeRequest MUST only be revoked as long as it is in REQUEST_PENDING status
        this.actionRequestService.revokeActionRequest(actionRequestId.iri(), idProvider.createUniqueLoUri().iri());
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.noContent().headers(headers).build();
    }
}
