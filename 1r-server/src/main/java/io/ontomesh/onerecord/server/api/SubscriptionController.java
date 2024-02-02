package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import io.ontomesh.onerecord.server.services.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.SubscriptionService;
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

@Slf4j
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(LogisticsObjectService logisticsObjectService,
                                  SubscriptionService subscriptionService,
                                  ActionRequestService actionRequestService) {
        super(logisticsObjectService, null, actionRequestService, null, null);
        this.subscriptionService = subscriptionService;
    }

    /**
     * the subscriber initiates the subscription process by actively sending subscription information to the publisher.
     * The subscription information can either specify a type of Logistics Object or a specific Logistics Object.
     * The publisher creates a SubscriptionRequest from the submitted Subscription.
     *
     * @param subscription
     * @return
     */
    @PostMapping(produces = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE},
            consumes = {SemanticWebMediaType.APPLICATION_JSON_LD_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE,
                    SemanticWebMediaType.TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> subscribe(@RequestBody LogisticsObject subscription) {

        String loType = getLoType(subscription);
        log.info("lo type :{}", loType);
        boolean hasId = isIdPresent(subscription);
        if (hasId) {
            checkForDuplicateIRI(subscription);
        }
        removeMarkerPredicate(subscription);
        //validate

        // save
        IRI actionRequestID = this.actionRequestService
                .createActionRequest(subscription, API.SubscriptionRequest);
        // update location.
        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(URI.create(subscriptionRequest.iri().stringValue()));
        headers.setContentType(SemanticWebMediaType.APPLICATION_JSON_LD);
        headers.add("Type", API.SubscriptionRequest.stringValue());

        return ResponseEntity.created(URI.create(actionRequestID.stringValue())).headers(headers).build();
    }


    /**
     * The publisher wants to notify the subscriber about changes to Logistics Objects of a certain type
     * (e.g. Waybill or Piece)
     *
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

        LogisticsObject subscriptionProposal = this.subscriptionService
                .createSubscriptionProposalResponse(topicIRI, topicTypeIRI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SemanticWebMediaType.APPLICATION_JSON_LD);
        headers.setContentLanguage(Locale.US);

        return ResponseEntity.ok().headers(headers).body(subscriptionProposal);
    }

}
