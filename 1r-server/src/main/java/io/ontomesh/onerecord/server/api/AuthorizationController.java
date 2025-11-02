package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.authorization.AuthorizationService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * REST controller for managing Authorization ACLs.
 */
@RestController
@RequestMapping("/authorizations-acls")
public class AuthorizationController extends BaseController {

    private final AuthorizationService authorizationService;

    /**
     * Constructs an AuthorizationController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     * @param logisticsObjectService Service for logistics object operations
     * @param authorizationService Service for authorization operations
     */
    public AuthorizationController(IdProvider idProvider,
                                   LogisticsObjectRevisionService logisticsObjectRevisionService,
                                   LogisticsObjectService logisticsObjectService,
                                   AuthorizationService authorizationService) {
        super(idProvider, logisticsObjectRevisionService, logisticsObjectService, null, null, null, null);
        this.authorizationService = authorizationService;
    }

    /**
     * Assigns an ACL to a logistics object.
     *
     * @param logisticsObject The logistics object to assign the ACL to
     * @return ResponseEntity with location header
     */
    @PostMapping(produces = {}, consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> assignACL(@Valid @RequestBody LogisticsObject logisticsObject) {

        LogisticsObjectService logisticsObjectService = getLogisticsObjectService();

        logisticsObjectService.saveLogisticsObject(logisticsObject);

        return ResponseEntity.created(logisticsObject.uri()).build();
    }


    /**
     * Fetches the ACL by ID.
     *
     * @param aclId The ACL ID
     * @param httpServletRequest The HTTP servlet request
     * @return The logistics object representing the ACL
     */
    @GetMapping(value = "/{aclId}", produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> getACL(@PathVariable("aclId") String aclId,
                                                  HttpServletRequest httpServletRequest) {
        IRI iri = getLoFromRequest(httpServletRequest);
        LogisticsObject logisticsObject = this.authorizationService.getACL(iri);
        return ResponseEntity.ok()
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT))))
                .body(logisticsObject);
    }


    /**
     * Deletes the ACL by ID.
     *
     * @param aclId The ACL ID
     * @param httpServletRequest The HTTP servlet request
     * @return ResponseEntity with no content
     */
    @DeleteMapping(value = "/{aclId}", produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> deleteACL(@PathVariable("aclId") String aclId,
                                                     HttpServletRequest httpServletRequest) {
        IRI iri = getLoFromRequest(httpServletRequest);
        this.authorizationService.deleteACL(iri);
        return ResponseEntity.ok().build();
    }
}

