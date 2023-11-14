package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.common.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/notifications")
public class NotificationController extends BaseController {


    @PostMapping(value = "", consumes =
            {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE},
            produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> handleNotification(HttpServletRequest httpServletRequest,
                                                              @RequestBody LogisticsObject logisticsObject) {

        //
        IRI logisticsEventIRI = null;
        HttpHeaders headers = null;
        return ResponseEntity.created(URI.create(logisticsEventIRI.stringValue()))
                .headers(headers)
                .build();
    }
}
