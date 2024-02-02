package io.ontomesh.onerecord.server;

import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.SubscriptionService;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
public class SubscriptionTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Test
    public void fetchSubscriptionById() {
        IRI subsriptionIRI = Values.iri("http://localhost:8085/logistics-objects/adebf621-829b-4e19-8746-59e2abe0fe67");
        LogisticsObject subscription = subscriptionService.getSubscription(subsriptionIRI);
        log.info("subscription id {}", subscription.iri());
        RDFUtil.print(subscription);
        assertEquals(subscription.type().stringValue(), API.Subscription.stringValue());
    }

    @Test
    public void fetchSubscriptionByLo() {

        IRI logisticsObjectIRI = Values.iri("http://localhost:8085/logistics-objects/9a1e4687-0d18-4839-b16c-5258e1db28ed");

        LogisticsObject subscriptionsByLoId = subscriptionService.getSubscriptionsByLoId(logisticsObjectIRI);
        RDFUtil.print(subscriptionsByLoId);
        int itemCount = Models.objectLiteral(
                        subscriptionsByLoId.model()
                                .filter(subscriptionsByLoId.iri(), OntoMesh.hasTotalItems, null))
                .orElse(Values.literal(0))
                .intValue();
        assertEquals(1, itemCount);
    }

    @Test
    public void fetchSubscriptionsByLoType() {

        IRI loTypeIRI = Values.iri("https://onerecord.iata.org/ns/cargo#Piece");

        LogisticsObject subscriptionsByLoId = subscriptionService
                .getSubscriptionsByLoType(loTypeIRI, API.LOGISTICS_OBJECT_TYPE);

        RDFUtil.print(subscriptionsByLoId);
        int itemCount = Models.objectLiteral(
                        subscriptionsByLoId.model()
                                .filter(subscriptionsByLoId.iri(), OntoMesh.hasTotalItems, null))
                .orElse(Values.literal(0))
                .intValue();

        assertEquals(1, itemCount);
    }

    @Test
    public void getSubscriberForLoTest() {
        IRI hasTopic = Values.iri("http://1r.ontomesh.io:8085/logistics-objects/7529be44-8fb4-419a-9267-2db9cce0ca3e");
        IRI hasTopicType = API.LOGISTICS_OBJECT_IDENTIFIER;

        List<IRI> subscribersForLo = subscriptionService.getSubscribers(hasTopic, hasTopicType);
        assertEquals(2, subscribersForLo.size());
    }
}
