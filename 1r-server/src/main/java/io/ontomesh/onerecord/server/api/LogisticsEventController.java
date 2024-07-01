package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.domain.LogisticsEvents;
import io.ontomesh.onerecord.server.services.LogisticsEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

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
    @GetMapping(path = "/{logisticsEventId}", produces = {APPLICATION_JSON_LD_VALUE,
            APPLICATION_TURTLE_VALUE,
            TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsEvent> getLogisticsEvent(HttpServletRequest httpServletRequest,
                                                             @PathVariable("logisticsObjectId") String logisticsObjectId,
                                                             @PathVariable("logisticsEventId") String logisticsEventId) {

//        IRI requestIRI = Values.iri(httpServletRequest.getRequestURL().toString());
//        log.debug("Request IRI {}", requestIRI);
        OntoMeshId id = getLogisticsEventId(logisticsObjectId, logisticsEventId);
        LogisticsEvent logisticsEvent = this.eventService.getLogisticsEvent(id.iri());

        //Last-Modified, Type, Location, Content-Type, Content-Language
        HttpHeaders headers = new HttpHeaders();
        headers.add("Type", CARGO.LogisticsEvent.stringValue());
        headers.setContentLanguage(Locale.US);
        headers.setLocation(URI.create(logisticsEvent.iri().stringValue()));
        headers.setLastModified(Instant.now()); //TODO check this value.
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));

        return ResponseEntity.ok().headers(headers).body(logisticsEvent);
    }

    private OntoMeshId getLogisticsEventId(String logisticsObjectId, String logisticsEventId) {
        return idProvider.getUriForLogisticsEventId(logisticsObjectId, logisticsEventId);
    }

    /**
     * Queries the logistic events.
     *
     * @param httpServletRequest
     * @param loReferenceId
     * @param eventType
     * @return
     */
    @GetMapping(produces = {APPLICATION_JSON_LD_VALUE,
            APPLICATION_TURTLE_VALUE,
            TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsEvents> getLogisticsEvents(HttpServletRequest httpServletRequest,
                                                               @PathVariable("logisticsObjectId") String loReferenceId,
                                                               @RequestParam(required = false, name = "eventType") String eventType) {
        OntoMeshId loId = getLoId(loReferenceId);

        LogisticsEvents events = this.eventService
                .getEventsByLogisticsObjectIRI(loId.iri(), eventType);

        //Last-Modified, Type, Location, Content-Type, Content-Language
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLanguage(Locale.US);
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));
        headers.setLastModified(Instant.now());

        return ResponseEntity.ok().headers(headers).body(events);
    }

    /**
     * Saves the logistics event.
     *
     * @param logisticsEvent
     * @param httpServletRequest
     * @return
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

        HttpHeaders headers = new HttpHeaders();
        headers.add("Type", CARGO.LogisticsEvent.stringValue());
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));

        return ResponseEntity.created(URI.create(logisticsEventIRI.stringValue())).headers(headers).build();
    }
}
