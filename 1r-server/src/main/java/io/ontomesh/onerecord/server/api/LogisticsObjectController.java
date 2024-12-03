package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.api.enums.RequestType;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import io.ontomesh.onerecord.server.services.AuditTrailService;
import io.ontomesh.onerecord.server.services.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.LogisticsObjectValidationService;
import io.ontomesh.onerecord.server.services.SnapshotService;
import io.ontomesh.onerecord.server.services.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_TURTLE_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.TEXT_TURTLE_VALUE;

@Slf4j
@RestController
@RequestMapping("/logistics-objects")
public class LogisticsObjectController extends BaseController {

    @Autowired
    private LogisticsObjectValidationService validationService;

    @Autowired
    private SubscriptionService subscriptionService;

    public LogisticsObjectController(LogisticsObjectService logisticsObjectService,
                                     SnapshotService snapshotService, AuditTrailService auditTrailService,
                                     ActionRequestService actionRequestService) {
        super(logisticsObjectService, snapshotService, actionRequestService, auditTrailService);
    }

    @GetMapping()
    private ResponseEntity<List<String>> getLogisticsObjects(@RequestParam(value = "type", required = false) IRI type,
                                                             @RequestParam(value = "limit", defaultValue = "20") int limit,
                                                             @RequestParam(value = "offset", defaultValue = "0") int offset) {

        List<IRI> iriList = this.getLogisticsObjectService().listAllLogisticsObjects(type, limit, offset);
        if (iriList != null) {
            return ResponseEntity.ok()
                    .body(iriList.stream().map(Object::toString).collect(Collectors.toList()));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLanguage(Locale.US);
        return ResponseEntity.ok()
                .headers(headers)
                .build();
    }

    @GetMapping(path = "/{loId}/subscriptions",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE,
                    TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsObject> getLogisticsSubscriptions(HttpServletRequest httpServletRequest,
                                                                      @PathVariable("loId") String loReferenceId) {
        OntoMeshId loId = getLoId(loReferenceId);

        LogisticsObject logisticsObject = this.subscriptionService
                .getSubscriptionsByLoId(loId.iri());

        //Last-Modified, Type, Location, Content-Type, Content-Language
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLanguage(Locale.US);
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));
        headers.setLastModified(Instant.now());

        return ResponseEntity.ok().headers(headers).body(logisticsObject);
    }

    /**
     * returns the Logistics object
     *
     * @param httpServletRequest http servlet request
     * @param loReferenceId      logistics object id
     * @param at                 snapshot at time (time is in YYYYMMDDThhmmssZ format)
     * @param embedded           Optional parameter that can be used to request an embedded version of a Logistics Object,
     *                           if the parameter is not set, a linked version of the Logistics Object is returned
     * @return Logistics Object
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
     * returns the LogisticsObject's Snapshot 'at' a specific time.
     *
     * @param at        snapshot time.
     * @param id        LogisticObject's reference.
     * @param mediaType Content Type.
     * @return response.
     */
    protected ResponseEntity<LogisticsObject> getLogisticsObjectSnapshot(String at, OntoMeshId id, MediaType mediaType) {
        log.debug("finding the snapshot for the lo id {} at {}", id.uri(), at);

        LogisticsObject logisticsObject = this.snapshotService.findSnapshot(id.iri(), at);
        Literal snapshotVersion = logisticsObject.revision();
        Integer latestRevision = logisticsObjectRevisionService.getRevision(id.iri());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLanguage(Locale.US);
        headers.setLastModified(Instant.now()); // TODO
        headers.setLocation(id.uri());
        headers.add("Type", logisticsObject.type().stringValue());
        headers.add("Revision", String.valueOf(snapshotVersion.intValue()));
        headers.add("Latest-Revision", latestRevision.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(logisticsObject);
    }

    /**
     * returns the latest logisticsObject
     *
     * @param id        logisticsObject reference
     * @param mediaType content type.
     * @return Response Entity.
     */

    protected ResponseEntity<LogisticsObject> getLogisticsObject(OntoMeshId id, MediaType mediaType) {

        LogisticsObject logisticsObject = super.getLogisticsObjectService().findLogisticsObject(id.iri());
        String revision = String.valueOf(logisticsObject.revision());
        log.debug("The logisticsObject {} revision is {} ", logisticsObject.iri(), revision);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLanguage(Locale.US);
        headers.setLastModified(Instant.now()); // TODO
        headers.setLocation(id.uri());
        headers.add("Type", logisticsObject.type().stringValue());
        headers.add("Revision", revision);
        headers.add("Latest-Revision", revision);

        return ResponseEntity.ok()
                .headers(headers)
                .body(logisticsObject);
    }

    /**
     * Returns the audit trail.
     *
     * @return logistics object audit.
     */
    @GetMapping(path = "/{id}/audit-trail",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> findLogisticsObjectAudit(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable("id") String loReferenceId) {

        OntoMeshId id = getLoId(loReferenceId);
        LogisticsObject logisticsObject = auditTrailService.getAuditTrail(id.uri());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLanguage(Locale.US);
        headers.setContentType(StringUtils.hasLength(accept) ? MediaType.valueOf(accept) : APPLICATION_JSON_LD);

        return ResponseEntity.ok()
                .headers(headers)
                .body(logisticsObject);
    }

    /**
     * Saves the logistics object
     *
     * @param logisticsObject logistics object
     * @return void
     */
    @PostMapping(
            consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE},
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> saveLogisticsObject(@RequestBody LogisticsObject logisticsObject) {

        boolean hasId = isIdPresent(logisticsObject);
        if (hasId) {
            checkForDuplicateIRI(logisticsObject);
        }
        removeMarkerPredicate(logisticsObject);
//        addRevisionToLO(logisticsObject);

        getLogisticsObjectService().saveLogisticsObject(logisticsObject);

        URI loURI = logisticsObject.uri();
        log.debug("{} is created successfully.", logisticsObject);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(loURI);
        headers.add("Type", logisticsObject.type().stringValue());

        return ResponseEntity.created(loURI).headers(headers).build();
    }

    /**
     * Update logistics objects
     *
     * @param change        change logistics object.
     * @param loReferenceId logistics objects loReferenceId.
     * @return void
     */
    @PatchMapping(path = "/{id}", consumes = {APPLICATION_JSON_LD_VALUE},
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> updateLogisticObject(@RequestBody LogisticsObject change,
                                                     @PathVariable("id") String loReferenceId) {

        OntoMeshId loId = getLoId(loReferenceId);
        this.validationService.validate(loId.iri(), change.model(), change.iri());
        IRI actionRequest = this.actionRequestService.createActionRequest(change, RequestType.ChangeRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON_LD);
        URI location = URI.create(actionRequest.stringValue());
        headers.setLocation(location);
        headers.add("Type", API.ChangeRequest.stringValue());

        return ResponseEntity.created(location).headers(headers).build();
    }
}
