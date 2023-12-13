package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.AlreadyExistsException;
import io.ontomesh.onerecord.server.exception.InvalidAddressException;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import io.ontomesh.onerecord.server.services.AuditTrailService;
import io.ontomesh.onerecord.server.services.LogisticsEventService;
import io.ontomesh.onerecord.server.services.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.SnapshotService;
import io.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * @author arun
 */
@Slf4j
public abstract class BaseController {

    @Autowired
    public IdProvider idProvider;
    public LogisticsObjectService logisticsObjectService;
    public SnapshotService snapshotService;
    public ActionRequestService actionRequestService;
    public LogisticsEventService eventService;
    public AuditTrailService auditTrailService;

    private final SimpleValueFactory instance = SimpleValueFactory.getInstance();

    private final IRI hasPredefinedIri = instance.createIRI("https://www.ontomesh.com/onerecord#", "hasId");

    public BaseController() {

    }

    /**
     * constructor.
     *
     * @param logisticsObjectService logistics object service.
     * @param snapshotService        snapshot service.
     * @param actionRequestService   action request service.
     * @param eventService           event service.
     * @param auditTrailService      audit trail service.
     */
    public BaseController(LogisticsObjectService logisticsObjectService,
                          SnapshotService snapshotService, ActionRequestService actionRequestService,
                          LogisticsEventService eventService, AuditTrailService auditTrailService) {
        this.logisticsObjectService = logisticsObjectService;
        this.snapshotService = snapshotService;
        this.actionRequestService = actionRequestService;
        this.eventService = eventService;
        this.auditTrailService = auditTrailService;
    }

    /**
     * Constructor.
     *
     * @param logisticsObjectService logistics object service.
     * @param snapshotService        snapshot service.
     * @param auditTrailService      audit trail service.
     */
    public BaseController(LogisticsObjectService logisticsObjectService,
                          SnapshotService snapshotService, AuditTrailService auditTrailService) {
        this.logisticsObjectService = logisticsObjectService;
        this.snapshotService = snapshotService;
        this.auditTrailService = auditTrailService;
    }

    /**
     * Controller.
     *
     * @param eventService event service.
     */
    public BaseController(LogisticsEventService eventService) {
        this.eventService = eventService;
    }

    /**
     * @param actionRequestService action service
     */
    public BaseController(ActionRequestService actionRequestService) {
        this.actionRequestService = actionRequestService;
    }

    public BaseController(LogisticsObjectService logisticsObjectService, SnapshotService snapshotService,
                          ActionRequestService actionRequestService, AuditTrailService auditTrailService) {
        this(logisticsObjectService, snapshotService, auditTrailService);
        this.actionRequestService = actionRequestService;
    }

    /**
     * remove hasPredefinedIri IRI.
     *
     * @param logisticsObject logistics object.
     */
    public void removeMarkerPredicate(LogisticsObject logisticsObject) {
        logisticsObject.model().remove(logisticsObject.iri(), hasPredefinedIri, null);
    }

    protected String getLoType(LogisticsObject logisticsObject) {
        Model model = logisticsObject.model();
        IRI iri = logisticsObject.iri();
        return RDFUtil.getType(model, iri).stringValue();
    }

    protected OntoMeshId getLoId(String uuid) {
        return idProvider.getUriForLoId(uuid);
    }

    protected OntoMeshId getActionRequestId(String uuid) {
        return idProvider.getUriForActionRequestId(uuid);
    }

    protected void checkForDuplicateIRI(LogisticsObject logisticsObject) {
        IRI iri = logisticsObject.iri();
        // Check for proper address path.
        if (!iri.toString().contains(idProvider.getLogisticsObjectBaseIRI().toString())) {
            throw new InvalidAddressException(iri, idProvider.getLogisticsObjectBaseIRI());
        }

        // Check if IRI is already taken.
        boolean exists = logisticsObjectService.isExists(iri);
        if (exists)
            throw new AlreadyExistsException(iri, getLoType(logisticsObject));
    }

    protected boolean isIdPresent(LogisticsObject logisticsObject) {
        // Remove predicate temporarily introduced by LogisticsObjectBodyReader. Information is still part
        // of metadata.
        return Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), hasPredefinedIri, null))
                .orElse(Values.literal(false))
                .booleanValue();
    }


    protected HttpHeaders getResponseHeaders(URI loURI, String loType, MediaType contentType) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(OntoMesh.LO_TYPE_HEADER, OntoMesh.LO_TYPE_COMPANY_INFORMATION);
        responseHeaders.setContentType(contentType);
        responseHeaders.add("Type", loType);
        responseHeaders.setLocation(loURI);
        return responseHeaders;
    }


    public HttpHeaders getResponseHeaders(LogisticsObject logisticsObject, MediaType contentType) {
        URI loURI = URI.create(logisticsObject.iri().stringValue());
        String loType = getLoType(logisticsObject);
        return getResponseHeaders(loURI, loType, contentType);
    }

    protected HttpHeaders getResponseHeaders(HttpServletRequest httpServletRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));
        headers.setLocation(URI.create(httpServletRequest.getRequestURL().toString()));
        headers.add("Revision", UUID.randomUUID().toString());
        headers.add(OntoMesh.LO_TYPE_HEADER, OntoMesh.LO_TYPE_COMPANY_INFORMATION);
        return headers;
    }

    /**
     * Add revision statement logistics object.
     *
     * @param logisticsObject logistics object.
     */
    public void addRevisionToLO(LogisticsObject logisticsObject) {
        logisticsObject.model().add(logisticsObject.iri(), API.hasRevision, Values.literal(1));
    }

    /**
     * returns the latest logisticsObject
     *
     * @param id        logisticsObject reference
     * @param mediaType content type.
     * @return Response Entity.
     */

    protected ResponseEntity<LogisticsObject> getLogisticsObject(OntoMeshId id, MediaType mediaType) {

        HttpHeaders headers = new HttpHeaders();
        LogisticsObject logisticsObject = logisticsObjectService.findLogisticsObject(id.uri());
        String revision = String.valueOf(logisticsObjectService.getRevision(logisticsObject));
        log.debug("The logisticsObject {} revision is {} ", logisticsObject.iri(), revision);
        headers.setContentType(mediaType);
        headers.setContentLanguage(Locale.US);
        headers.setLastModified(Instant.now()); // TODO
        headers.setLocation(id.uri());
        headers.add("Type", logisticsObject.type().stringValue());
        headers.add("Latest-Revision", revision);
        return ResponseEntity.ok().headers(headers).body(logisticsObject);
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

        HttpHeaders headers = new HttpHeaders();
        log.debug("finding the snapshot for the lo id {} at {}", id.uri(), at);


        LogisticsObject logisticsObject = this.snapshotService.findSnapshot(id.iri(), at);
        int snapshotVersion = snapshotService.getRevision(logisticsObject);
        Integer latestRevision = logisticsObjectService.getRevision(id.iri());

        headers.setContentType(mediaType);
        headers.setContentLanguage(Locale.US);
        headers.setLastModified(Instant.now()); // TODO
        headers.setLocation(id.uri());
        headers.add("Type", logisticsObject.type().stringValue());
        headers.add("Revision", String.valueOf(snapshotVersion));
        headers.add("Latest-Revision", latestRevision.toString());
        return ResponseEntity.ok().headers(headers).body(logisticsObject);
    }
}
