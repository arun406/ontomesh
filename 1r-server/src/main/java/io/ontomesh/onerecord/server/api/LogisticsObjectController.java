package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.actionrequests.ActionRequestService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectValidationUtil;
import io.ontomesh.onerecord.server.services.logisticsobjects.audittrail.AuditTrailService;
import io.ontomesh.onerecord.server.services.logisticsobjects.snapshot.SnapshotService;
import io.ontomesh.onerecord.server.services.subscriptions.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * REST controller for managing Logistics Objects and related operations.
 */
@Slf4j
@RestController
@RequestMapping("/logistics-objects")
public class LogisticsObjectController extends BaseController {


    private final SubscriptionService subscriptionService;

    /**
     * Constructs a LogisticsObjectController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     * @param logisticsObjectService Service for logistics object operations
     * @param snapshotService Service for snapshot operations
     * @param auditTrailService Service for audit trail operations
     * @param actionRequestService Service for action request operations
     * @param subscriptionService Service for subscription operations
     */
    public LogisticsObjectController(IdProvider idProvider,
                                     LogisticsObjectRevisionService logisticsObjectRevisionService,
                                     LogisticsObjectService logisticsObjectService,
                                     SnapshotService snapshotService,
                                     AuditTrailService auditTrailService,
                                     ActionRequestService actionRequestService,
                                     SubscriptionService subscriptionService) {
        super(idProvider, logisticsObjectRevisionService, logisticsObjectService, snapshotService, actionRequestService, null, auditTrailService);
        this.subscriptionService = subscriptionService;
    }

    /**
     * Retrieves a list of logistics object IRIs, optionally filtered by type.
     *
     * @param type   Optional IRI type filter
     * @param limit  Maximum number of results to return
     * @param offset Offset for pagination
     * @return List of logistics object IRIs as strings
     */
    @GetMapping()
    public ResponseEntity<List<String>> getLogisticsObjects(@RequestParam(value = "type", required = false) IRI type,
                                                           @RequestParam(value = "limit", defaultValue = "20") int limit,
                                                           @RequestParam(value = "offset", defaultValue = "0") int offset) {

        List<IRI> iriList = this.getLogisticsObjectService().listAllLogisticsObjects(type, limit, offset);
        if (iriList != null) {
            return ResponseEntity.ok()
                    .body(iriList.stream().map(Object::toString).collect(Collectors.toList()));
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the subscriptions for a given logistics object.
     *
     * @param httpServletRequest The HTTP servlet request
     * @return The logistics object with subscription information
     */
    @GetMapping(path = "/{loId}/subscriptions",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> getLogisticsSubscriptions(HttpServletRequest httpServletRequest) {

        IRI iri = getLoFromRequest(httpServletRequest);
        LogisticsObject logisticsObject = this.subscriptionService.getSubscriptionsByLoId(iri);

        return ResponseEntity.ok().body(logisticsObject);
    }

    /**
     * Retrieves a logistics object by ID, optionally at a specific snapshot in time.
     *
     * @param httpServletRequest The HTTP servlet request
     * @param loReferenceId      The logistics object ID
     * @param at                 Optional snapshot time (YYYYMMDDThhmmssZ)
     * @param embedded           Whether to return an embedded version
     * @return The logistics object
     */
    @GetMapping(path = "/{id}",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> findLogisticsObject(HttpServletRequest httpServletRequest,
                                                               @PathVariable("id") String loReferenceId,
                                                               @RequestParam(required = false, name = "at") String at,
                                                               @RequestParam(required = false, name = "embedded") boolean embedded) {
        OntoMeshId id = getLoId(loReferenceId);
        log.info("Searching the logistics objects with IRI of : {}", id.uri());

        MediaType contentType = MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT));
        if (at != null) {
            return getLogisticsObjectSnapshot(at, id, contentType);
        } else {
            return getLogisticsObject(id, contentType);
        }
    }

    /**
     * Retrieves a snapshot of a logistics object at a specific time.
     *
     * @param at        The snapshot time
     * @param id        The logistics object reference
     * @param mediaType The content type
     * @return The logistics object snapshot
     */
    protected ResponseEntity<LogisticsObject> getLogisticsObjectSnapshot(String at, OntoMeshId id, MediaType mediaType) {
        log.debug("finding the snapshot for the lo id {} at {}", id.uri(), at);
        LogisticsObject logisticsObject = this.snapshotService.findSnapshot(id.iri(), at);
        return ResponseEntity.ok().body(logisticsObject);
    }

    /**
     * Retrieves the latest version of a logistics object.
     *
     * @param id        The logistics object reference
     * @param mediaType The content type
     * @return The latest logistics object
     */
    protected ResponseEntity<LogisticsObject> getLogisticsObject(OntoMeshId id, MediaType mediaType) {
        
        LogisticsObject logisticsObject = super.getLogisticsObjectService().findLogisticsObject(id.iri());

        log.debug("The logisticsObject {} revision is {} ", logisticsObject.iri(), logisticsObject.revision());

        return ResponseEntity.ok().body(logisticsObject);
    }

    /**
     * Retrieves the audit trail for a logistics object.
     *
     * @param accept              The Accept header value
     * @param httpServletRequest  The HTTP servlet request
     * @return The logistics object audit trail
     */
    @GetMapping(path = "/{id}/audit-trail",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> findLogisticsObjectAudit(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            HttpServletRequest httpServletRequest) {

        IRI iri = getLoFromRequest(httpServletRequest);
        LogisticsObject logisticsObject = auditTrailService.getAuditTrail(iri);

        return ResponseEntity.ok().body(logisticsObject);
    }

    /**
     * Saves a new logistics object.
     *
     * @param logisticsObject The logistics object to save
     * @return ResponseEntity with location header
     */
    @PostMapping(
            consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE},
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> saveLogisticsObject(@Valid @RequestBody LogisticsObject logisticsObject) {

        boolean hasId = isIdPresent(logisticsObject);
        if (hasId) {
            checkForDuplicateIRI(logisticsObject);
        }
        removeMarkerPredicate(logisticsObject);

        getLogisticsObjectService().saveLogisticsObject(logisticsObject);
        log.debug("{} is created successfully.", logisticsObject.iri());

        URI location = logisticsObject.uri();

        return ResponseEntity.created(location).build();
    }

    /**
     * Updates an existing logistics object.
     *
     * @param change             The changes to apply
     * @param httpServletRequest The HTTP servlet request
     * @return ResponseEntity with location header
     */
    @PatchMapping(path = "/{id}", consumes = {APPLICATION_JSON_LD_VALUE},
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> updateLogisticObject(@Valid @RequestBody LogisticsObject change,
                                                     HttpServletRequest httpServletRequest) {

        IRI iri = getLoFromRequest(httpServletRequest);
        LogisticsObjectValidationUtil.validate(iri, change.model(), change.iri());
        IRI actionRequest = getActionRequestService().createChangeRequest(change);

        URI location = URI.create(actionRequest.stringValue());

        return ResponseEntity.created(location).build();
    }
}
