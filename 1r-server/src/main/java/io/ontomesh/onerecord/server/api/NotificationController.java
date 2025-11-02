package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * REST controller for receiving notifications as Logistics Objects.
 */
@Slf4j
@RestController
@RequestMapping(value = "/notifications",
        consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE},
        produces = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
public class NotificationController extends BaseController {

    /**
     * Constructs a NotificationController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     */
    public NotificationController(IdProvider idProvider,
                                  LogisticsObjectRevisionService logisticsObjectRevisionService) {
        super(idProvider, logisticsObjectRevisionService, null, null, null, null, null);
    }

    /**
     * Handles incoming notifications as Logistics Objects.
     *
     * @param logisticsObject The logistics object notification
     * @return ResponseEntity with no content
     */
    @PostMapping()
    public ResponseEntity<LogisticsObject> handleNotification(@Valid @RequestBody LogisticsObject logisticsObject) {
        RDFUtil.print(logisticsObject);
        return ResponseEntity.noContent().build();
    }
}
