package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.exception.ServerInfoNotFoundException;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.services.server.ServerInformationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for providing server information.
 */
@Slf4j
@RestController
@RequestMapping(path = "/")
public class ServerInformationController extends BaseController {

    private final ServerInformationService serverInformationService;
    @Value("${onerecord.server.url:http://server.ontomesh.io}")
    private String ONERECORD_SERVER_BASE;

    /**
     * Constructs a ServerInformationController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     * @param serverInformationService Service for server information operations
     */
    public ServerInformationController(IdProvider idProvider,
                                       LogisticsObjectRevisionService logisticsObjectRevisionService,
                                       ServerInformationService serverInformationService) {
        super(idProvider, logisticsObjectRevisionService, null, null, null, null, null);
        this.serverInformationService = serverInformationService;
    }

    /**
     * Returns the One Record server information.
     *
     * @param request The HTTP servlet request
     * @return The logistics object representing server information
     */
    @GetMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> getServerInformation(HttpServletRequest request) {
        try {
            LogisticsObject logisticsObject = serverInformationService.getServerInformation(ONERECORD_SERVER_BASE);
            return ResponseEntity.ok().body(logisticsObject);
        } catch (LogisticsObjectNotFoundException logisticsObjectNotFoundException) {
            throw new ServerInfoNotFoundException("Server Information is not defined.");
        }
    }
}
