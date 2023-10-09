package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import com.ontomesh.onerecord.server.exception.LogisticsObjectNotSupportedException;
import com.ontomesh.onerecord.server.services.ServerInformationService;
import com.ontomesh.onerecord.server.services.SubscriptionService;
import com.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController extends BaseController {

    public static final String LOGISTICS_OBJECT_TYPE = "LOGISTICS_OBJECT_TYPE";
    @Autowired
    private final SubscriptionService subscriptionService;
    private final ServerInformationService serverInformationService;

    public SubscriptionController(SubscriptionService subscriptionService, ServerInformationService serverInformationService) {
        this.subscriptionService = subscriptionService;
        this.serverInformationService = serverInformationService;
    }

    /**
     * @param contentType
     * @param input
     * @return
     * @throws IOException
     */
    @PostMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE}, consumes = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<String> subscribe(LogisticsObject logisticsObject, @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                            @RequestBody String input) throws IOException {

//        LogisticsObject logisticsObject = getLogisticObject(contentType, input);
        String loType = getLoType(logisticsObject);
        log.info("lo type :{}", loType);
        boolean hasId = isIdPresent(logisticsObject);
        if (hasId) {
            checkForDuplicateIRI(logisticsObject);
        }
        removeMarkerPredicate(logisticsObject);
        //validate

        // save
        this.subscriptionService.createSubscriptionRequest(logisticsObject);

        URI loURI = URI.create(logisticsObject.iri().toString());
        HttpHeaders headers = getResponseHeaders(loURI, loType, SemanticWebMediaType.APPLICATION_JSON_LD);
        return ResponseEntity
                .created(loURI)
                .headers(headers)
                .build();
    }


    /**
     * @param topic
     * @param topicType
     * @param accept
     * @return
     */
    @GetMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<String> handleProposal(@RequestParam("topic") String topic,
                                                 @RequestParam("topicType") String topicType,
                                                 @RequestHeader(HttpHeaders.ACCEPT) MediaType accept,
                                                 @RequestHeader(HttpHeaders.CONTENT_TYPE) MediaType contentType) {
        if (topicType == null || topicType.isBlank() ||
                topic == null || topic.isBlank()) {
            throw new InvalidApiRequestException("Missing query parameter 'topicType' or 'topic'.");
        }

        if (!Arrays.asList("LOGISTICS_OBJECT_TYPE", "LOGISTICS_OBJECT_URI").contains(topicType)) {
            throw new InvalidApiRequestException("Invalid 'topicType'.");
        }

        if (LOGISTICS_OBJECT_TYPE.equalsIgnoreCase(topicType)) {
            Set<String> supportedLogisticTypes = this.serverInformationService.getSupportedLogisticTypes();
            if (!supportedLogisticTypes.contains(topic)) {
                throw new LogisticsObjectNotSupportedException("Provided Logistics Object Type is not supported");
            }
        }
        LogisticsObject subscriptionProposal = this.subscriptionService
                .createSubscriptionProposalResponse(topic, topicType);

        OutputStream stream = RDFUtil.getRDFByteStream(subscriptionProposal.model(), accept);
        HttpHeaders headers = getResponseHeaders(contentType);
        return ResponseEntity.ok()
                .headers(headers)
                .body(stream.toString());
    }
}

