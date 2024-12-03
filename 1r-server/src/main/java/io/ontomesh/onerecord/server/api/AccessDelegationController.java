package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.api.enums.RequestType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import io.ontomesh.onerecord.server.util.URIUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_TURTLE_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.TEXT_TURTLE_VALUE;

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
    @PostMapping(produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> createRequest(@RequestBody LogisticsObject accessDelegation) {

        IRI type = accessDelegation.type();

        if (!type.equals(API.AccessDelegation)) {
            throw new IllegalArgumentException(String.format("Invalid request type %s", type));
        }

        IRI actionRequestID = actionRequestService
                .createActionRequest(accessDelegation, RequestType.AccessDelegationRequest);
        log.debug("action request with IRI {} created successfully.", actionRequestID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON_LD);
        headers.add("Type", String.valueOf(API.AccessDelegation));
        return ResponseEntity.created(URIUtil.toURI(actionRequestID)).headers(headers).build();
    }
}
