package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.HttpHeadersBuilder;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.domain.LogisticsEvents;
import io.ontomesh.onerecord.server.services.logisticsobjects.events.LogisticsEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_TURTLE_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.TEXT_TURTLE_VALUE;

@Slf4j
@RestController
@RequestMapping("/logistics-objects/{logisticsObjectId}/logistics-events")
public class LogisticsEventController extends BaseController {


    /**
     * constructor.
     *
     * @param eventService
     */
    public LogisticsEventController(LogisticsEventService eventService) {
        super(eventService);
    }

    /**
     * Get single logistics event by iri.
     *
     * @param httpServletRequest
     * @return
     */
    @GetMapping(path = "/{logisticsEventId}",
            produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsEvent> getLogisticsEvent(HttpServletRequest httpServletRequest,
                                                             @PathVariable("logisticsObjectId") String logisticsObjectId,
                                                             @PathVariable("logisticsEventId") String logisticsEventId) {

        OntoMeshId id = getLogisticsEventId(logisticsObjectId, logisticsEventId);
        LogisticsEvent logisticsEvent = this.eventService.getLogisticsEvent(id.iri());

        //Last-Modified, Type, Location, Content-Type, Content-Language
        HttpHeaders headers = new HttpHeadersBuilder()
                .type(CARGO.LogisticsEvent.stringValue())
                .contentLanguage(Locale.US)
                .location(URI.create(logisticsEvent.iri().stringValue()))
                .lastModified(Instant.now())
                .contentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)))
                .build();

        return ResponseEntity.ok().headers(headers).body(logisticsEvent);
    }

    private OntoMeshId getLogisticsEventId(String logisticsObjectId, String logisticsEventId) {
        return idProvider.getUriForLogisticsEventId(logisticsObjectId, logisticsEventId);
    }

    /**
     * Retrieves all logistics events associated with a specific logistics object.
     *
     * @param httpServletRequest The HTTP request object.
     * @param loReferenceId      The ID of the logistics object.
     * @param eventType          The type of event to filter by (optional).
     * @return A ResponseEntity containing the list of logistics events.
     */
    @GetMapping(produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsEvents> getLogisticsEvents(HttpServletRequest httpServletRequest,
                                                               @PathVariable("logisticsObjectId") String loReferenceId,
                                                               @RequestParam(required = false, name = "eventType") String eventType) {
        OntoMeshId loId = getLoId(loReferenceId);

        LogisticsEvents events = this.eventService
                .getEventsByLogisticsObjectIRI(loId.iri(), eventType);

        //Last-Modified, Type, Location, Content-Type, Content-Language
        HttpHeaders headers = new HttpHeadersBuilder()
                .contentLanguage(Locale.US)
                .contentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)))
                .lastModified(Instant.now()).build();

        return ResponseEntity.ok().headers(headers).body(events);
    }

    /**
     * Save logistics events.
     * <p>
     * This endpoint allows the creation of new logistics events associated with a specific logistics object.
     *
     * @param logisticsEvent     The logistics event to be saved.
     * @param logisticsObjectId  The ID of the logistics object to which the event is related.
     * @param httpServletRequest The HTTP request object.
     * @return A ResponseEntity with a status of 201 Created and headers containing the location of the created resource.
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

        HttpHeaders headers = new HttpHeadersBuilder()
                .type(CARGO.LogisticsEvent.stringValue())
                .contentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)))
                .build();

        return ResponseEntity.created(URI.create(logisticsEventIRI.stringValue())).headers(headers).build();
    }
}
