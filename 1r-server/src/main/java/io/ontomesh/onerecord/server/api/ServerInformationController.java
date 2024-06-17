package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.exception.ServerInfoNotFoundException;
import io.ontomesh.onerecord.server.services.ServerInformationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping(path = "/")
public class ServerInformationController extends BaseController {

    @Autowired
    private final ServerInformationService serverInformationService;
    @Value("${onerecord.server.url:http://1r.ontomesh.io}")
    private String ONERECORD_SERVER_BASE;

    public ServerInformationController(ServerInformationService serverInformationService) {
        super();
        this.serverInformationService = serverInformationService;
    }

    /**
     * return the one record server information.
     *
     * @return Server information.
     */
    @GetMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> getServerInformation(HttpServletRequest request) {

        try {
            String accept = request.getHeader(HttpHeaders.ACCEPT);
            LogisticsObject logisticsObject = serverInformationService.getServerInformation(ONERECORD_SERVER_BASE);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLanguage(Locale.US);
            headers.setContentType(StringUtils.hasLength(accept) ? MediaType.valueOf(accept) : SemanticWebMediaType.APPLICATION_JSON_LD);
            headers.setLastModified(Instant.now());

            return ResponseEntity.ok().headers(headers).body(logisticsObject);
        } catch (LogisticsObjectNotFoundException logisticsObjectNotFoundException) {
            throw new ServerInfoNotFoundException("Server Information is not defined.");
        }
    }
}
