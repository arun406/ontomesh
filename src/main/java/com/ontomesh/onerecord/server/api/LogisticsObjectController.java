package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.common.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.services.AuditTrailService;
import com.ontomesh.onerecord.server.services.LogisticsObjectService;
import com.ontomesh.onerecord.server.services.LogisticsObjectValidationService;
import com.ontomesh.onerecord.server.services.SnapshotService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/logistics-objects")
public class LogisticsObjectController extends BaseController {

    private final LogisticsObjectService logisticsObjectService;
    private final LogisticsObjectValidationService validationService;
    private final AuditTrailService auditTrailService;
    private final SnapshotService snapshotService;

    /**
     * returns the Logistics object
     *
     * @param httpServletRequest http servlet request
     * @param uri                logistics object id
     * @param at                 snapshot at
     * @return Logistics Object
     */
    @GetMapping(path = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsObject> findLogisticsObject(HttpServletRequest httpServletRequest,
                                                                @PathVariable("id") String uri,
                                                                @RequestParam(required = false, name = "at") String at) {
        OntoMeshId loId = getLoId(uri);
        log.info("Searching the logistics objects with IRI of : {}", loId.uri());

        HttpHeaders headers = getResponseHeaders(httpServletRequest);
        LogisticsObject logisticsObject;

        if (at != null) {
            log.debug("finding the snapshot for the lo id {} at {}", loId.uri(), at);
            logisticsObject = this.snapshotService.findSnapshot(loId.iri(), at);

            headers.add("Type", logisticsObject.type().stringValue());
            headers.remove("Location");
            headers.add("Location", logisticsObject.iri().stringValue());
            int snapshotVersion = snapshotService.getRevision(logisticsObject);
            headers.add("Revision", String.valueOf(snapshotVersion));
            Integer latestRevision = logisticsObjectService.getRevision(loId.iri());
            headers.add("Latest-Revision", latestRevision.toString());
            headers.remove("LO-type");
            headers.remove("Revision-Header");
        } else {
            URI requestURI = URI.create(httpServletRequest.getRequestURL().toString());
            logisticsObject = logisticsObjectService.findLogisticsObject(requestURI);
        }

        return ResponseEntity.ok().headers(headers).body(logisticsObject);
    }

    /**
     * Returns the audit trail.
     *
     * @param httpServletRequest http servlet request
     * @return logistics object audit.
     */
    @GetMapping(path = "/{id}/audit-trail", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsObject> findLogisticsObjectAudit(HttpServletRequest httpServletRequest) {
        log.info("Finding the audit trail of logistics objects with IRI of : {}", httpServletRequest.getRequestURL());
        URI requestURI = URI.create(httpServletRequest.getRequestURL().toString());
        LogisticsObject logisticsObject = this.auditTrailService.getAuditTrail(requestURI);

        HttpHeaders headers = getResponseHeaders(httpServletRequest);
        return ResponseEntity.ok().headers(headers).body(logisticsObject);
    }

    /**
     * Saves the logistics object
     *
     * @param logisticsObject logistics object
     * @return void
     */
    @PostMapping(consumes =
            {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> saveLogisticsObject(@RequestBody LogisticsObject logisticsObject) {

        boolean hasId = isIdPresent(logisticsObject);
        if (hasId) {
            checkForDuplicateIRI(logisticsObject);
        }
        removeMarkerPredicate(logisticsObject);
        addRevisionToLO(logisticsObject);
        this.logisticsObjectService.saveLogisticsObject(logisticsObject);
        URI loURI = URI.create(logisticsObject.iri().toString());
        log.debug("Logistics Object is created with IRI : {}", loURI);
        HttpHeaders responseHeaders = getResponseHeaders(logisticsObject, SemanticWebMediaType.APPLICATION_JSON_LD);
        return ResponseEntity.created(loURI).headers(responseHeaders).build();
    }


    /**
     * Update logistics objects
     *
     * @param change change logistics object.
     * @param id     logistics objects id.
     * @return void
     */
    @PatchMapping(path = "/{id}", consumes = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> updateLogisticObject(@RequestBody LogisticsObject change,
                                                     @PathVariable("id") String id) {

        log.debug("updating the {} logistics object ", id);
        OntoMeshId loId = getLoId(id);
        this.validationService.validate(change, loId.iri());

        LogisticsObject logisticsObject = this.logisticsObjectService.findLogisticsObject(loId.uri());
        OntoMeshId uriForLoId = idProvider.getUriForLoId(id);
        Set<IRI> iris = this.logisticsObjectService.updateLogisticObject(uriForLoId, change, logisticsObject);

        LogisticsObject snapshot = this.snapshotService.createSnapshot(logisticsObject);
        log.debug("snapshot with {} is created.", snapshot.iri());
        HttpHeaders responseHeaders = getResponseHeaders(change, SemanticWebMediaType.APPLICATION_JSON_LD);
        return ResponseEntity.noContent().headers(responseHeaders).build();
    }
}
