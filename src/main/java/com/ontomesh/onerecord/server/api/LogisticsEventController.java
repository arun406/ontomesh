package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.common.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.services.LogisticsEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/logistics-objects/{loId}/logistics-events")
@RequiredArgsConstructor
public class LogisticsEventController extends BaseController {

    private final LogisticsEventService eventService;

    @PostMapping(value = "", consumes =
            {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> saveLogisticsEvents(@PathVariable("loId") String loReferenceId,
                                                    @RequestBody LogisticsObject logisticsEvent) {

        OntoMeshId loId = getLoId(loReferenceId);
        log.debug("LO ID {}", loId);
        // validate loId.
        IRI logisticsEventIRI = this.eventService.saveLogisticsEvent(loId.iri(), logisticsEvent);
        log.debug("Logistics Event IRI {}", logisticsEventIRI);
        return ResponseEntity.created(URI.create(logisticsEventIRI.stringValue())).build();
    }

    @GetMapping(path = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsObject> getLogisticsEvent(@PathVariable("loId") String loReferenceId,
                                                              @PathVariable("id") String eventId, HttpServletRequest httpServletRequest) {

        IRI requestIRI = Values.iri(httpServletRequest.getRequestURL().toString());
        log.debug("Request IRI {}", requestIRI);
        LogisticsObject logisticsEvent = this.eventService.getLogisticsEvent(requestIRI);

        HttpHeaders headers = null; //Last-Modified, Type, Location, Content-Type, Content-Language
        return ResponseEntity.ok().headers(headers).body(logisticsEvent);
    }

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
                .getLogisticsEventByLogisticsObject(loId.iri(), requestURI, eventType);

        HttpHeaders headers = null; //Last-Modified, Type, Location, Content-Type, Content-Language
        return ResponseEntity.ok().headers(headers).body(logisticsObject);
    }
}
