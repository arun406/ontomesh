package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.AlreadyExistsException;
import io.ontomesh.onerecord.server.services.actionrequests.ActionRequestService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.logisticsobjects.audittrail.AuditTrailService;
import io.ontomesh.onerecord.server.services.logisticsobjects.events.LogisticsEventService;
import io.ontomesh.onerecord.server.services.logisticsobjects.snapshot.SnapshotService;
import io.ontomesh.onerecord.server.util.URIUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * @author arun
 */
@Slf4j
public abstract class BaseController {

    protected final IdProvider idProvider;
    protected final LogisticsObjectRevisionService logisticsObjectRevisionService;
    protected final LogisticsObjectService logisticsObjectService;
    protected final SnapshotService snapshotService;
    protected final ActionRequestService actionRequestService;
    protected final LogisticsEventService eventService;
    protected final AuditTrailService auditTrailService;

    /**
     * Main constructor for all dependencies. Any dependency not needed by a subclass can be passed as null.
     */
    protected BaseController(
            IdProvider idProvider,
            LogisticsObjectRevisionService logisticsObjectRevisionService,
            LogisticsObjectService logisticsObjectService,
            SnapshotService snapshotService,
            ActionRequestService actionRequestService,
            LogisticsEventService eventService,
            AuditTrailService auditTrailService
    ) {
        this.idProvider = idProvider;
        this.logisticsObjectRevisionService = logisticsObjectRevisionService;
        this.logisticsObjectService = logisticsObjectService;
        this.snapshotService = snapshotService;
        this.actionRequestService = actionRequestService;
        this.eventService = eventService;
        this.auditTrailService = auditTrailService;
    }

    /**
     * Remove hasPredefinedIri IRI.
     */
    protected void removeMarkerPredicate(LogisticsObject logisticsObject) {
        logisticsObject.model().remove(logisticsObject.iri(), OntoMesh.hasId, null);
    }

    /**
     * Returns the logistics object id.
     */
    protected OntoMeshId getLoId(String uuid) {
        return idProvider.getUriForLoId(uuid);
    }

    /**
     * Returns the action request id.
     */
    protected OntoMeshId getActionRequestId(String uuid) {
        return idProvider.getUriForActionRequestId(uuid);
    }

    /**
     * Perform the duplicate check.
     */
    protected void checkForDuplicateIRI(LogisticsObject logisticsObject) {
        IRI iri = logisticsObject.iri();
        boolean exists = logisticsObjectService != null && logisticsObjectService.isExists(iri);
        if (exists)
            throw new AlreadyExistsException(iri, logisticsObject.type().toString());
    }

    /**
     * Checks if the ID is present.
     */
    protected boolean isIdPresent(LogisticsObject logisticsObject) {
        return Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), OntoMesh.hasId, null))
                .orElse(Values.literal(false))
                .booleanValue();
    }

    /**
     * Extracts IRI from request.
     */
    protected IRI getLoFromRequest(HttpServletRequest httpServletRequest) {
        URI uri = ServletUriComponentsBuilder.fromRequestUri(httpServletRequest)
                .build()
                .toUri();
        return URIUtil.toIRI(uri);
    }

    // Protected getters for all services
    protected LogisticsObjectService getLogisticsObjectService() { return logisticsObjectService; }
    protected ActionRequestService getActionRequestService() { return actionRequestService; }
    protected LogisticsEventService getEventService() { return eventService; }
    protected AuditTrailService getAuditTrailService() { return auditTrailService; }
    protected SnapshotService getSnapshotService() { return snapshotService; }
    protected LogisticsObjectRevisionService getLogisticsObjectRevisionService() { return logisticsObjectRevisionService; }
    protected IdProvider getIdProvider() { return idProvider; }
}
