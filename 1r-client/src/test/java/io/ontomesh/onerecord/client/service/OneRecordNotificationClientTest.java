package io.ontomesh.onerecord.client.service;

import io.ontomesh.onerecord.client.provider.IdProvider;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class OneRecordNotificationClientTest {

    @Autowired
    private IdProvider idProvider;
    @Autowired
    private NotificationClientService notificationClientService;

    @Test
    void contextLoads() {
    }

    @Test
    public void sendNotification() {

        Model model = new ModelBuilder()
                .setNamespace("api", "https://onerecord.iata.org/ns/api#")
                .setNamespace("cargo", "https://onerecord.iata.org/ns/cargo#")
                .subject(idProvider.createInternalIri().iri())
                .add(RDF.TYPE, Values.iri("api:Notification"))
                .add("api:hasLogisticsObject", Values.iri("https://1r.example.com/logistics-objects/1a8ded38-1804-467c-a369-81a411416b7c"))
                .add("api:hasEventType", Values.iri("api:LOGISTICS_OBJECT_CREATED"))
                .add("api:isTriggeredBy", Values.iri("https://1r.example.com/action-requests/599fea49-7287-42af-b441-1fa618d2aaed"))
                .add("api:hasLogisticsObjectType", Values.iri("https://onerecord.iata.org/ns/cargo#Piece"))
                .build();

        notificationClientService.sendNotification(model);
    }

}
