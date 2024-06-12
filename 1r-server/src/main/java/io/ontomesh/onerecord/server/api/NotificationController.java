package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * @author arun kumar
 * API to receive the notifications.
 */
@Slf4j
@RestController
@RequestMapping(value = "/notifications",
        consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE},
        produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
public class NotificationController extends BaseController {

    public NotificationController() {
        super();
    }

    @PostMapping()
    public ResponseEntity<LogisticsObject> handleNotification(@RequestBody LogisticsObject logisticsObject) {
        RDFUtil.print(logisticsObject);
        return ResponseEntity.noContent().build();
    }
}
