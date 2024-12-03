package io.ontomesh.events.processor;

import io.ontomesh.events.processor.service.SubscriptionService;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootTest(classes = EventProcessorApplication.class)
public class ListAllSubscribersTest {

    @Autowired
    private SubscriptionService service;

    @Test
    void listAllSubscribersTest() {
        IRI loId = Values.iri("http://server.ontomesh.io/logistics-objects/3b13c319-bbb4-48a7-bfee-3ce00813a155");
        IRI loType = Values.iri("https://onerecord.iata.org/ns/cargo#Shipment");
        IRI eventType = Values.iri("https://onerecord.iata.org/ns/api#23LOGISTICS_OBJECT_CREATED");
        service.listSubscribers(loId, null, eventType);
    }
}
