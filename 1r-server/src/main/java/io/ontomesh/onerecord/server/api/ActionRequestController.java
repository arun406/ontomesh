package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.OntomeshBadRequestException;
import io.ontomesh.onerecord.server.services.actionrequests.ActionRequestService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * REST controller for managing Action Requests.
 */
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
     * Constructs an ActionRequestController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     * @param actionRequestService Service for action request operations
     */
    public ActionRequestController(IdProvider idProvider,
                                   LogisticsObjectRevisionService logisticsObjectRevisionService,
                                   ActionRequestService actionRequestService) {
        super(idProvider, logisticsObjectRevisionService, null, null, actionRequestService, null, null);
    }

    /**
     * Lists all action requests, optionally filtered by type and status.
     *
     * @param type   The type of action request
     * @param status The status of the action request
     * @param offset The page offset
     * @param limit  The page size
     * @return List of ActionRequest objects
     */
    @GetMapping(produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<List<ActionRequest>>
    listAll(
            @RequestParam(value = "type", required = false, defaultValue = "https://onerecord.iata.org/ns/api#ChangeRequest") String type,
            @RequestParam(value = "status", required = false, defaultValue = "https://onerecord.iata.org/ns/api#REQUEST_PENDING") String status,
            @RequestParam(value = "page", defaultValue = "0") int offset,
            @RequestParam(value = "size", defaultValue = "20") int limit) {

        List<ActionRequest> actionRequests = this.actionRequestService
                .listAll(SimpleValueFactory.getInstance().createIRI(type), limit, offset, SimpleValueFactory.getInstance().createIRI(status));

        return ResponseEntity.ok().body(actionRequests);
    }

    /**
     * Retrieves an action request by ID.
     *
     * @param actionRequestReference The action request reference ID
     * @return The logistics object representing the action request
     */
    @GetMapping(value = "/{id}", produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> get(@PathVariable("id") String actionRequestReference) {

        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        LogisticsObject actionRequest = actionRequestService.get(actionRequestId.iri());

        return ResponseEntity.ok().body(actionRequest);
    }

    /**
     * Updates the status of an action request.
     *
     * @param actionRequestReference The action request reference ID
     * @param status                 The new status
     * @return ResponseEntity with no content
     */
    @PatchMapping(value = "/{id}",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> update(@PathVariable("id") String actionRequestReference,
                                       @RequestParam(required = false, name = "status") String status) {

        if (!StringUtils.hasText(status)) {
            throw new OntomeshBadRequestException("status cannot be blank.");
        }
        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        IRI newStatus = null;

        try {
            newStatus = Values.iri(status);
        } catch (IllegalArgumentException iae) {
            if (!VALID_ACTION_REQUEST_STATUSES.contains(Values.iri(API.NAMESPACE + status))) {
                throw new OntomeshBadRequestException("Invalid status.");
            }
            newStatus = Values.iri(API.NAMESPACE + status);
        }

        this.actionRequestService.updateStatus(actionRequestId.iri(), newStatus);
        return ResponseEntity.noContent().build();
    }

    /**
     * Revokes an action request by ID.
     *
     * @param actionRequestReference The action request reference ID
     * @return ResponseEntity with no content
     */
    @DeleteMapping(value = "/{id}", produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> delete(@PathVariable("id") String actionRequestReference) {

        OntoMeshId actionRequestId = getActionRequestId(actionRequestReference);
        log.debug("deleting action request of id {} ", actionRequestId.iri());

        // A ChangeRequest MUST only be revoked as long as it is in REQUEST_PENDING status
        this.actionRequestService.revokeActionRequest(actionRequestId.iri(), idProvider.createUniqueLoUri().iri());
        return ResponseEntity.noContent().build();
    }
}
