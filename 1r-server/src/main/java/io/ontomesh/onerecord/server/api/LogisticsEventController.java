package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.domain.LogisticsEvents;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.services.logisticsobjects.events.LogisticsEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * REST controller for managing Logistics Events associated with Logistics Objects.
 */
@Slf4j
@RestController
@RequestMapping("/logistics-objects/{logisticsObjectId}/logistics-events")
public class LogisticsEventController extends BaseController {


    /**
     * Constructs a LogisticsEventController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     * @param eventService Service for logistics event operations
     */
    public LogisticsEventController(IdProvider idProvider,
                                    LogisticsObjectRevisionService logisticsObjectRevisionService,
                                    LogisticsEventService eventService) {
        super(idProvider, logisticsObjectRevisionService, null, null, null, eventService, null);
    }

    /**
     * Retrieves a single logistics event by its ID.
     *
     * @param httpServletRequest The HTTP servlet request
     * @param logisticsObjectId  The logistics object ID
     * @param logisticsEventId   The logistics event ID
     * @return The logistics event
     */
    @GetMapping(path = "/{logisticsEventId}",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsEvent> getLogisticsEvent(HttpServletRequest httpServletRequest,
                                                             @PathVariable("logisticsObjectId") String logisticsObjectId,
                                                             @PathVariable("logisticsEventId") String logisticsEventId) {

        OntoMeshId id = getLogisticsEventId(logisticsObjectId, logisticsEventId);
        LogisticsEvent logisticsEvent = this.eventService.getLogisticsEvent(id.iri());

        return ResponseEntity.ok().body(logisticsEvent);
    }

    private OntoMeshId getLogisticsEventId(String logisticsObjectId, String logisticsEventId) {
        return idProvider.getUriForLogisticsEventId(logisticsObjectId, logisticsEventId);
    }

    /**
     * Retrieves all logistics events associated with a specific logistics object.
     *
     * @param httpServletRequest The HTTP servlet request
     * @param loReferenceId      The logistics object ID
     * @param eventType          Optional event type filter
     * @return The logistics events
     */
    @GetMapping(produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsEvents> getLogisticsEvents(HttpServletRequest httpServletRequest,
                                                               @PathVariable("logisticsObjectId") String loReferenceId,
                                                               @RequestParam(required = false, name = "eventType") String eventType) {
        OntoMeshId loId = getLoId(loReferenceId);

        LogisticsEvents events = this.eventService
                .getEventsByLogisticsObjectIRI(loId.iri(), eventType);

        return ResponseEntity.ok().body(events);
    }

    /**
     * Saves a new logistics event associated with a specific logistics object.
     *
     * @param logisticsEvent     The logistics event to save
     * @param logisticsObjectId  The logistics object ID
     * @param httpServletRequest The HTTP servlet request
     * @return ResponseEntity with location header
     */
    @PostMapping(consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE},
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> saveLogisticsEvents(@RequestBody LogisticsEvent logisticsEvent,
                                                    @PathVariable("logisticsObjectId") String logisticsObjectId,
                                                    HttpServletRequest httpServletRequest) {

        OntoMeshId loId = getLoId(logisticsObjectId);
        // validate loId.
        IRI logisticsEventIRI = this.eventService.saveLogisticsEvent(loId.iri(), logisticsEvent);
        log.debug("Logistics Event IRI {}", logisticsEventIRI);

        return ResponseEntity.created(URI.create(logisticsEventIRI.stringValue())).build();
    }
}
