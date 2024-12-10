package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.AuthorizationService;
import io.ontomesh.onerecord.server.services.LogisticsObjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.APPLICATION_TURTLE_VALUE;
import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.TEXT_TURTLE_VALUE;

@RestController
@RequestMapping("/authorizations-acls")
public class AuthorizationController extends BaseController {

    private final AuthorizationService authorizationService;

    /**
     * Constructor.
     *
     * @param logisticsObjectService logistics object service.
     */
    public AuthorizationController(LogisticsObjectService logisticsObjectService, AuthorizationService authorizationService) {
        super(logisticsObjectService);
        this.authorizationService = authorizationService;
    }

    @PostMapping(produces = {}, consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> assignACL(@RequestBody LogisticsObject logisticsObject) {

        LogisticsObjectService logisticsObjectService = getLogisticsObjectService();

        logisticsObjectService.saveLogisticsObject(logisticsObject);

        return ResponseEntity.created(logisticsObject.uri())
                .headers(httpHeaders -> {
                    httpHeaders.setLocation(logisticsObject.uri());
                    httpHeaders.add("Type", "http://www.w3.org/ns/auth/acl#Authorization");
                })
                .build();
    }


    /**
     * Fetch the ACL by Id
     *
     * @param aclId
     * @param httpServletRequest
     * @return
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
     * Delete the ACL by Id
     *
     * @param aclId
     * @param httpServletRequest
     * @return
     */
    @DeleteMapping(value = "/{aclId}", produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> deleteACL(@PathVariable("aclId") String aclId,
                                                     HttpServletRequest httpServletRequest) {
        IRI iri = getLoFromRequest(httpServletRequest);
        this.authorizationService.deleteACL(iri);
        return ResponseEntity.ok().build();
    }
}

