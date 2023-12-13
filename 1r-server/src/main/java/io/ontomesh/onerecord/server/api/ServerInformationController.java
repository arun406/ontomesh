package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.exception.ServerInfoNotFoundException;
import io.ontomesh.onerecord.server.services.ServerInformationService;
import io.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;

@Slf4j
@RestController
@RequestMapping(path = "/")
public class ServerInformationController extends BaseController {

    @Autowired
    private final ServerInformationService serverInformationService;

    public ServerInformationController(ServerInformationService serverInformationService) {
        super();
        this.serverInformationService = serverInformationService;
    }

    /**
     * return the one record server information.
     *
     * @param accept
     * @param httpServletRequest
     * @return
     */
    @GetMapping(path = "", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<String> getServerInformation(@RequestHeader(HttpHeaders.ACCEPT) MediaType accept,
                                                       HttpServletRequest httpServletRequest) {

        try {
            LogisticsObject logisticsObject = serverInformationService.getServerInformation();
            OutputStream stream = RDFUtil.getRDFByteStream(logisticsObject.model(), accept);
            HttpHeaders headers = getResponseHeaders(httpServletRequest);
            return ResponseEntity.ok().headers(headers).body(stream.toString());
        } catch (LogisticsObjectNotFoundException logisticsObjectNotFoundException) {
            throw new ServerInfoNotFoundException("Server Information is not defined.");
        }
    }
}
