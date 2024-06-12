package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import io.ontomesh.onerecord.server.services.ActionRequestService;
import io.ontomesh.onerecord.server.services.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

@Slf4j
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(LogisticsObjectService logisticsObjectService,
                                  SubscriptionService subscriptionService,
                                  ActionRequestService actionRequestService) {
        super(logisticsObjectService, actionRequestService);
        this.subscriptionService = subscriptionService;
    }

    /**
     * the subscriber initiates the subscription process by actively sending subscription information to the publisher.
     * The subscription information can either specify a type of Logistics Object or a specific Logistics Object.
     * The publisher creates a SubscriptionRequest from the submitted Subscription.
     *
     * @param subscription Subscription Logistics Object
     * @return
     */
    @RequestMapping(method = RequestMethod.POST,
            produces = {APPLICATION_JSON_LD_VALUE},
            consumes = {APPLICATION_JSON_LD_VALUE, APPLICATION_TURTLE_VALUE, TEXT_TURTLE_VALUE})
    public ResponseEntity<Void> subscribe(@RequestBody LogisticsObject subscription) {

        boolean hasId = isIdPresent(subscription);
        if (hasId) {
            checkForDuplicateIRI(subscription);
        }
        removeMarkerPredicate(subscription);
        //validate
        IRI topicType = subscription.findIRI(API.hasTopicType, false);
        Value topicValue = subscription.findValue(API.hasTopic);
        if (topicValue.isIRI()) {
            IRI topic = (IRI) topicValue;
            validate(topic, topicType);
        }
        // save
        IRI actionRequestID = this.actionRequestService
                .createActionRequest(subscription, API.SubscriptionRequest);
        // update location.
        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(URI.create(subscriptionRequest.iri().stringValue()));
        headers.setContentType(APPLICATION_JSON_LD);
        headers.add("Type", API.SubscriptionRequest.stringValue());

        return ResponseEntity.created(URI.create(actionRequestID.stringValue())).headers(headers).build();
    }


    /**
     * The publisher wants to notify the subscriber about changes to Logistics Objects of a certain type
     * (e.g. Waybill or Piece)
     *
     * @param topic     topic
     * @param topicType topic type
     * @return
     */
    @GetMapping(produces = {APPLICATION_JSON_LD_VALUE,
            APPLICATION_TURTLE_VALUE,
            TEXT_TURTLE_VALUE})
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
        validate(topicIRI, topicTypeIRI);

        LogisticsObject subscriptionProposal = this.subscriptionService
                .createSubscriptionProposalResponse(topicIRI, topicTypeIRI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON_LD);
        headers.setContentLanguage(Locale.US);

        return ResponseEntity.ok().headers(headers).body(subscriptionProposal);
    }

    /**
     * Validates the topic and topic type.
     *
     * @param topic     topic
     * @param topicType topic type
     */
    private void validate(IRI topic, IRI topicType) {
        log.trace("validating the request parameters.");
        if (topicType == null || topic == null) {
            throw new InvalidApiRequestException("Missing query parameter 'topicType' or 'topic'.");
        }
        if (!Arrays.asList(API.LOGISTICS_OBJECT_TYPE, API.LOGISTICS_OBJECT_IDENTIFIER)
                .contains(topicType)) {
            throw new InvalidApiRequestException("Invalid 'topicType'.");
        }
        if (API.LOGISTICS_OBJECT_TYPE.equals(topicType)) {
            List<IRI> logisticsObjectTypes = getLogisticsObjectService().logisticsObjectTypes();
            if (logisticsObjectTypes != null) {
                log.debug("count of logistics object types : {}", logisticsObjectTypes.size());
                logisticsObjectTypes.stream()
                        .filter(iri -> iri.equals(topic))
                        .findAny()
                        .orElseThrow(() -> new InvalidApiRequestException(String.format("Invalid topic : %s .", topic)));
            }
        }
    }
}
