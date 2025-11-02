package io.ontomesh.onerecord.server.api;

import io.ontomesh.onerecord.server.api.enums.RequestType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import io.ontomesh.onerecord.server.services.actionrequests.ActionRequestService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectRevisionService;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectService;
import io.ontomesh.onerecord.server.services.subscriptions.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import static io.ontomesh.onerecord.server.common.SemanticWebMediaType.*;

/**
 * REST controller for managing Subscriptions.
 */
@Slf4j
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    /**
     * Constructs a SubscriptionController with required dependencies.
     *
     * @param idProvider IdProvider for generating and parsing IDs
     * @param logisticsObjectRevisionService Service for logistics object revision management
     * @param logisticsObjectService Service for logistics object operations
     * @param subscriptionService Service for subscription operations
     * @param actionRequestService Service for action request operations
     */
    public SubscriptionController(IdProvider idProvider,
                                  LogisticsObjectRevisionService logisticsObjectRevisionService,
                                  LogisticsObjectService logisticsObjectService,
                                  SubscriptionService subscriptionService,
                                  ActionRequestService actionRequestService) {
        super(idProvider, logisticsObjectRevisionService, logisticsObjectService, null, actionRequestService, null, null);
        this.subscriptionService = subscriptionService;
    }

    /**
     * Subscribes to a topic or logistics object.
     *
     * @param subscription The subscription logistics object
     * @return ResponseEntity with location header
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
                .createActionRequest(subscription, RequestType.SubscriptionRequest);
        // update location.
        return ResponseEntity.created(URI.create(actionRequestID.stringValue())).build();
    }


    /**
     * Handles a subscription proposal for a topic or logistics object type.
     *
     * @param topic     The topic IRI as a string
     * @param topicType The topic type IRI as a string
     * @return The logistics object representing the subscription proposal
     */
    @GetMapping(produces = {APPLICATION_JSON_LD_VALUE,
            APPLICATION_TURTLE_VALUE,
            TEXT_TURTLE_VALUE})
    public ResponseEntity<LogisticsObject> handleProposal(@RequestParam(value = "topic") String topic,
                                                          @RequestParam(value = "topicType") String topicType) {

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

        return ResponseEntity.ok().body(subscriptionProposal);
    }

    /**
     * Validates the topic and topic type for a subscription.
     *
     * @param topic     The topic IRI
     * @param topicType The topic type IRI
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
            Set<IRI> logisticsObjectTypes = getLogisticsObjectService().logisticsObjectTypes();
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
