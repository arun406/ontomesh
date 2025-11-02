package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.api.enums.RequestType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.OntomeshBadRequestException;
import io.ontomesh.onerecord.server.services.actionrequests.ActionRequestService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.util.URIUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * REST controller for managing Access Delegation requests.
 */
@Slf4j
@RestController
@RequestMapping("/access-delegations")
public class AccessDelegationController extends BaseController {

    /**
     * Constructs an AccessDelegationController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     * @param actionRequestService Service for action request operations
     */
    public AccessDelegationController(IdProvider idProvider,
                                      LogisticsObjectRevisionService logisticsObjectRevisionService,
                                      ActionRequestService actionRequestService) {
        super(idProvider, logisticsObjectRevisionService, null, null, actionRequestService, null, null);
    }

    /**
     * Creates an access delegation request.
     *
     * @param accessDelegation The logistics object representing the access delegation
     * @return ResponseEntity with location header
     */
    @PostMapping(produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> createRequest(@Valid @RequestBody LogisticsObject accessDelegation) {

        IRI type = accessDelegation.type();

        if (!type.equals(API.AccessDelegation)) {
            throw new OntomeshBadRequestException(String.format("Invalid request type %s", type));
        }

        IRI actionRequestID = actionRequestService
                .createActionRequest(accessDelegation, RequestType.AccessDelegationRequest);
        log.debug("action request with IRI {} created successfully.", actionRequestID);

        return ResponseEntity.created(URIUtil.toURI(actionRequestID)).build();
    }
}
