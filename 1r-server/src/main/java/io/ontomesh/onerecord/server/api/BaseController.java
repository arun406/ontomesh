package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.AlreadyExistsException;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import io.ontomesh.onerecord.server.services.AuditTrailService;
import io.ontomesh.onerecord.server.services.LogisticsEventService;
import io.ontomesh.onerecord.server.services.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.services.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.SnapshotService;
import io.ontomesh.onerecord.server.util.URIUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * @author arun
 */
@Slf4j
public abstract class BaseController {

    @Autowired
    public IdProvider idProvider;
    @Autowired
    public LogisticsObjectRevisionService logisticsObjectRevisionService;

    private LogisticsObjectService logisticsObjectService;
    public SnapshotService snapshotService;
    public ActionRequestService actionRequestService;
    public LogisticsEventService eventService;
    public AuditTrailService auditTrailService;

    public BaseController() {
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


    public BaseController(LogisticsObjectService logisticsObjectService, ActionRequestService actionRequestService) {
        this.logisticsObjectService = logisticsObjectService;
        this.actionRequestService = actionRequestService;
    }

    public BaseController(LogisticsObjectService logisticsObjectService) {
        this.logisticsObjectService = logisticsObjectService;
    }

    public BaseController(LogisticsObjectService logisticsObjectService, SnapshotService snapshotService,
                          AuditTrailService auditTrailService, ActionRequestService actionRequestService) {
        this(logisticsObjectService, snapshotService, auditTrailService);
        this.actionRequestService = actionRequestService;
    }

    /**
     * remove hasPredefinedIri IRI.
     *
     * @param logisticsObject logistics object.
     */
    public void removeMarkerPredicate(LogisticsObject logisticsObject) {
        logisticsObject.model().remove(logisticsObject.iri(), OntoMesh.hasId, null);
    }

    /**
     * returns the logistics object id
     *
     * @param uuid
     * @return
     */
    protected OntoMeshId getLoId(String uuid) {
        return idProvider.getUriForLoId(uuid);
    }

    /**
     * returns the action request id
     *
     * @param uuid
     * @return
     */
    protected OntoMeshId getActionRequestId(String uuid) {
        return idProvider.getUriForActionRequestId(uuid);
    }

    /**
     * perform the duplicate check
     *
     * @param logisticsObject
     */
    protected void checkForDuplicateIRI(LogisticsObject logisticsObject) {
        IRI iri = logisticsObject.iri();
        // Check if IRI is already taken.
        boolean exists = logisticsObjectService.isExists(iri);
        if (exists)
            throw new AlreadyExistsException(iri, logisticsObject.type().toString());
    }

    /**
     * @param logisticsObject
     * @return
     */
    protected boolean isIdPresent(LogisticsObject logisticsObject) {
        // Remove predicate temporarily introduced by LogisticsObjectBodyReader. Information is still part
        // of metadata.
        return Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), OntoMesh.hasId, null))
                .orElse(Values.literal(false))
                .booleanValue();
    }

    public IRI getLoFromRequest(HttpServletRequest httpServletRequest) {
        URI uri = ServletUriComponentsBuilder.fromRequestUri(httpServletRequest)
                .build()
                .toUri();
        return URIUtil.toIRI(uri);
    }

    protected LogisticsObjectService getLogisticsObjectService() {
        return this.logisticsObjectService;
    }

    protected ActionRequestService getActionRequestService() {
        return this.actionRequestService;
    }
}
