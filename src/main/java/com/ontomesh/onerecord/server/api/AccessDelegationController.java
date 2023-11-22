package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.common.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.services.ActionRequestService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/access-delegations")
public class AccessDelegationController extends BaseController {

    /**
     * constructor.
     *
     * @param actionRequestService action request service.
     */
    public AccessDelegationController(ActionRequestService actionRequestService) {
        super(actionRequestService);
    }

    /**
     * 1. An organization requests an access delegation for itself for the Piece with the URI.
     * 2. An organization requests an access delegation for a business partner for the Piece with the URI.
     * create Access Delegation request.
     *
     * @param accessDelegation access delegation.
     * @return void
     */
    @PostMapping(value = "", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> createRequest(@RequestBody LogisticsObject accessDelegation) {
        IRI type = accessDelegation.type();
        if (!type.stringValue().equals(API.AccessDelegation.stringValue())) {
            throw new RuntimeException(String.format("Invalid request type %s", type));
        }
        IRI actionRequestID = actionRequestService.createActionRequest(accessDelegation, API.AccessDelegation);
        log.debug("action request with IRI {} created successfully.", actionRequestID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SemanticWebMediaType.APPLICATION_JSON_LD);
        headers.add("Type", API.AccessDelegation.stringValue());
        return ResponseEntity.created(URI.create(actionRequestID.stringValue())).headers(headers).build();
    }
}
