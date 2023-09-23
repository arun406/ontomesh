package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.ServerInfoNotFoundException;
import com.ontomesh.onerecord.server.exception.SubjectNotFoundException;
import com.ontomesh.onerecord.server.services.ServerInformationService;
import com.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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

    private final ServerInformationService serverInformationService;
    private final ServerInformationService loService;


    public ServerInformationController(ServerInformationService serverInformationService, ServerInformationService loService) {
        this.serverInformationService = serverInformationService;
        this.loService = loService;
    }


    @GetMapping(path = "/", produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<String> getServerInformation(@RequestHeader(HttpHeaders.ACCEPT) MediaType accept,
                                                       HttpServletRequest httpServletRequest) {

        try {
            LogisticsObject logisticsObject = loService.getServerInformation();
            OutputStream stream = RDFUtil.getRDFByteStream(logisticsObject.model(), accept);
            HttpHeaders headers = getResponseHeaders(httpServletRequest);

            return ResponseEntity.ok().headers(headers).body(stream.toString());

        } catch (SubjectNotFoundException subjectNotFoundException) {
            throw new ServerInfoNotFoundException("Server Information is not defined.");
        }
    }
}
