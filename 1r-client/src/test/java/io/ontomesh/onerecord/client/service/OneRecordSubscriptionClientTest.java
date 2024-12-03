package io.ontomesh.onerecord.client.service;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OneRecordSubscriptionClientTest {

    @Autowired
    private SubscriptionClient clientService;

    @Test
    public void initiateSubscriptionTest() {
        IRI topic = Values.iri("http://localhost:8085/logistics-objects/99e16c24-df98-4527-979f-42d6bc208ffa");
        IRI topicType = Values.iri("https://onerecord.iata.org/ns/api#LOGISTICS_OBJECT_IDENTIFIER");

        Model model = this.clientService.initiateSubscription("http://server.ontomesh.io:8085", topic, topicType);
    }
}
