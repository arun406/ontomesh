package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.LogisticsObjectURINotMatchedException;
import com.ontomesh.onerecord.server.util.RDFUtil;
import com.ontomesh.onerecord.server.services.LoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URI;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/logistics-objects")
public class LogisticsObjectController extends BaseController {

    @Autowired
    private LoService loService;

    /**
     * @param httpServletRequest
     * @param uri
     * @return
     */
    @GetMapping(path = "/{id}", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity findLogisticsObject(@RequestHeader(HttpHeaders.ACCEPT) MediaType accept,
                                               HttpServletRequest httpServletRequest,
                                               @PathVariable("id") String uri) {

        log.info("request URI :{}", httpServletRequest.getRequestURI());

        OntoMeshId loId = getLoId(uri);
        log.info("URI to search : {}", loId.getUri());
        URI requestURI = URI.create(httpServletRequest.getRequestURL().toString());
        LogisticsObject logisticsObject = loService.findLogisticsObject(requestURI);
        OutputStream stream = RDFUtil.getRDFByteStream(logisticsObject.model(), accept);
        HttpHeaders headers = getResponseHeaders(httpServletRequest);
        return ResponseEntity.ok().headers(headers).body(stream.toString());
    }


    /**
     * @param contentType
     * @param input
     * @return
     * @throws IOException
     */
    @PostMapping(consumes =
            {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    private ResponseEntity saveLogisticsObject(@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                               @RequestBody String input) throws IOException {

        LogisticsObject logisticsObject = getLogisticObject(contentType, input);
        boolean hasId = isIdPresent(logisticsObject);
        if (hasId) {
            checkForDuplicateIRI(logisticsObject);
        }
        removeMarkerPredicate(logisticsObject);
        loService.saveLogisticsObject(logisticsObject);

        URI loURI = URI.create(logisticsObject.iri().toString());
        HttpHeaders responseHeaders = getResponseHeaders(loURI, logisticsObject, SemanticWebMediaType.APPLICATION_JSON_LD);
        return ResponseEntity.created(loURI)
                .headers(responseHeaders)
                .build();
    }

    /**
     * @param contentType
     * @param input
     * @param id
     * @return
     * @throws IOException
     */
    @PatchMapping(path = "/{id}", consumes = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity updateLogisticObject(@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                               @RequestBody String input,
                                               @PathVariable("id") String id) throws IOException {

        log.debug("lo id :{}", id);
        OntoMeshId loId = getLoId(id);
        URI pathId = loId.getUri();

        LogisticsObject logisticsObject = getLogisticObject(contentType, input);
        Model model = logisticsObject.model();
        Optional<IRI> optionalIRI = Models.objectIRI(model.filter(logisticsObject.iri(), API.hasLogisticsObject, null));
        Resource node = Models.objectResource(model.filter(logisticsObject.iri(), API.hasLogisticsObject, null)).orElse(null);
        URI loUri = URI.create(optionalIRI.orElseThrow(RuntimeException::new).stringValue());
        if (pathId.compareTo(loUri) != 0) {
            LogisticsObjectURINotMatchedException exception = new LogisticsObjectURINotMatchedException();
            exception.setIri(loId.getIri());
            throw exception;
        }
        this.loService.updateLogisticObject(idProvider.getUriForLoId(id), logisticsObject);
        HttpHeaders responseHeaders = getResponseHeaders(loUri, logisticsObject, SemanticWebMediaType.APPLICATION_JSON_LD);

        return ResponseEntity.noContent()
                .headers(responseHeaders)
                .build();
    }


}
