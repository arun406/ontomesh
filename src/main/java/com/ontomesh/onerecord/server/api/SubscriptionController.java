package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.common.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import com.ontomesh.onerecord.server.exception.LogisticsObjectNotSupportedException;
import com.ontomesh.onerecord.server.services.ServerInformationService;
import com.ontomesh.onerecord.server.services.SubscriptionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;
    private final ServerInformationService serverInformationService;

    /**
     * @param logisticsObject
     * @return
     */
    @PostMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE}, consumes = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> subscribe(@RequestBody LogisticsObject logisticsObject) {

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
        return ResponseEntity.created(loURI)
                .headers(headers)
                .build();
    }


    /**
     * @param topic
     * @param topicType
     * @return
     */
    @GetMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE,
            SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
            SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> handleProposal(@RequestParam(value = "topic", required = false) String topic,
                                                          @RequestParam(value = "topicType", required = false) String topicType) {

        if (topicType == null || topicType.isBlank() ||
                topic == null || topic.isBlank()) {
            throw new InvalidApiRequestException("Missing query parameter 'topicType' or 'topic'.");
        }

        topicType = URLDecoder.decode(topicType, StandardCharsets.UTF_8);
        topic = URLDecoder.decode(topic, StandardCharsets.UTF_8);
        log.debug("Topic type : {}, Topic url : {}", topicType, topic);
        IRI topicTypeIRI = Values.iri(topicType);
        IRI topicIRI = Values.iri(topic);

        if (!Arrays.asList(API.LOGISTICS_OBJECT_TYPE.stringValue(), API.LOGISTICS_OBJECT_IDENTIFIER.stringValue())
                .contains(topicType)) {
            throw new InvalidApiRequestException("Invalid 'topicType'.");
        }

        if (API.LOGISTICS_OBJECT_TYPE.stringValue().equals(topicType)) {
            Set<IRI> supportedLogisticTypes = this.serverInformationService.getSupportedLogisticTypes();
            if (!supportedLogisticTypes.contains(topicIRI)) {
                throw new LogisticsObjectNotSupportedException("Logistics Object Type is not supported");
            }
        }
        LogisticsObject subscriptionProposal = this.subscriptionService
                .createSubscriptionProposalResponse(topicIRI, topicTypeIRI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SemanticWebMediaType.APPLICATION_JSON_LD);
        headers.setContentLanguage(Locale.US);

        return ResponseEntity.ok()
                .headers(headers)
                .body(subscriptionProposal);
    }
}

