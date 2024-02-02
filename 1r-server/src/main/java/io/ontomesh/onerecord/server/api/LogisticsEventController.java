package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.LogisticsEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
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

@Slf4j
@RestController
@RequestMapping("/logistics-objects/{loId}/logistics-events")
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
    @GetMapping(path = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsObject> getLogisticsEvent(HttpServletRequest httpServletRequest) {

        IRI requestIRI = Values.iri(httpServletRequest.getRequestURL().toString());
        log.debug("Request IRI {}", requestIRI);
        LogisticsObject logisticsEvent = this.eventService.getLogisticsEvent(requestIRI);

        //Last-Modified, Type, Location, Content-Type, Content-Language
        HttpHeaders headers = new HttpHeaders();
        headers.add("Type", CARGO.LogisticsEvent.stringValue());
        headers.setContentLanguage(Locale.US);
        headers.setLocation(URI.create(logisticsEvent.iri().stringValue()));
        headers.setLastModified(Instant.now()); //TODO check this value.
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));

        return ResponseEntity.ok().headers(headers).body(logisticsEvent);
    }

    /**
     * Queries the logistic events.
     *
     * @param httpServletRequest
     * @param loReferenceId
     * @param eventType
     * @return
     */
    @GetMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsObject> getLogisticsEvents(HttpServletRequest httpServletRequest,
                                                               @PathVariable("loId") String loReferenceId,
                                                               @RequestParam(required = false, name = "eventType") String eventType) {
        URI requestURI = URI.create(httpServletRequest.getRequestURL().toString());
        log.debug("Request URI {}", requestURI);
        OntoMeshId loId = getLoId(loReferenceId);

        LogisticsObject logisticsObject = this.eventService
                .getEventsByLogisticsObjectIRI(loId.iri(), requestURI, eventType);

        //Last-Modified, Type, Location, Content-Type, Content-Language
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLanguage(Locale.US);
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));
        headers.setLastModified(Instant.now());

        return ResponseEntity.ok().headers(headers).body(logisticsObject);
    }

    /**
     * Saves the logistics event.
     *
     * @param logisticsEvent
     * @param httpServletRequest
     * @return
     */
    @PostMapping(consumes =
            {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> saveLogisticsEvents(@RequestBody LogisticsObject logisticsEvent,
                                                    HttpServletRequest httpServletRequest) {

        StringBuffer requestURL = httpServletRequest.getRequestURL();
        String loId = requestURL.substring(0, requestURL.lastIndexOf("/logistics-events"));
        IRI loIRI = Values.iri(loId);
        // validate loId.
        IRI logisticsEventIRI = this.eventService.saveLogisticsEvent(loIRI, CARGO.LogisticsEvent, logisticsEvent);
        log.debug("Logistics Event IRI {}", logisticsEventIRI);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Type", CARGO.LogisticsEvent.stringValue());
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));

        return ResponseEntity.created(URI.create(logisticsEventIRI.stringValue())).headers(headers).build();
    }
}
