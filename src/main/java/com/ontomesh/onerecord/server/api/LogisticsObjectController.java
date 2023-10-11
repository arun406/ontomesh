package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.services.LogisticsObjectService;
import com.ontomesh.onerecord.server.services.LogisticsObjectValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/logistics-objects")
public class LogisticsObjectController extends BaseController {

    private final LogisticsObjectService logisticsObjectService;
    private final LogisticsObjectValidationService validationService;

    /**
     * @param httpServletRequest
     * @param uri
     * @return
     */
    @GetMapping(path = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity<LogisticsObject> findLogisticsObject(@RequestHeader(HttpHeaders.ACCEPT) MediaType accept,
                                                                HttpServletRequest httpServletRequest,
                                                                @PathVariable("id") String uri) {
        OntoMeshId loId = getLoId(uri);
        log.info("URI to search : {}", loId.uri());
        URI requestURI = URI.create(httpServletRequest.getRequestURL().toString());
        LogisticsObject logisticsObject = logisticsObjectService.findLogisticsObject(requestURI);

//        String rdfString = new ModelToStringConverter(RDFUtil.getFormat(accept)).convert(logisticsObject.model());
        HttpHeaders headers = getResponseHeaders(httpServletRequest);

        return ResponseEntity.ok().headers(headers).body(logisticsObject);
    }


    /**
     * @return
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
        logisticsObjectService.saveLogisticsObject(logisticsObject);

        URI loURI = URI.create(logisticsObject.iri().toString());
        HttpHeaders responseHeaders = getResponseHeaders(logisticsObject, SemanticWebMediaType.APPLICATION_JSON_LD);
        return ResponseEntity.created(loURI).headers(responseHeaders).build();
    }


    /**
     * @param change
     * @param id
     * @return
     */
    @PatchMapping(path = "/{id}", consumes = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> updateLogisticObject(@RequestBody LogisticsObject change,
                                                     @PathVariable("id") String id) {

        log.debug("lo id :{}", id);
        OntoMeshId loId = getLoId(id);
        log.debug("Change Id : {}", change.iri());
        this.validationService.validate(change, loId.iri());

        this.logisticsObjectService.updateLogisticObject(idProvider.getUriForLoId(id), change);
        HttpHeaders responseHeaders = getResponseHeaders(change, SemanticWebMediaType.APPLICATION_JSON_LD);

        return ResponseEntity.noContent().headers(responseHeaders).build();
    }
}
