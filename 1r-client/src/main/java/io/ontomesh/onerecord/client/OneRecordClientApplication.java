package io.ontomesh.onerecord.client;

import io.ontomesh.onerecord.client.provider.IdProvider;
import io.ontomesh.onerecord.client.service.NotificationClientService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class OneRecordClientApplication implements CommandLineRunner {

    @Autowired
    private IdProvider idProvider;
    @Autowired
    private NotificationClientService notificationClientService;


    public static void main(String[] args) {
        log.info("STARTING THE APPLICATION");
        SpringApplication.run(OneRecordClientApplication.class, args);
        log.info("APPLICATION FINISHED");
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("EXECUTING : command line runner");
//        Model model = new ModelBuilder()
//                .setNamespace("api", "https://onerecord.iata.org/ns/cargo#")
//                .setNamespace("cargo", "https://onerecord.iata.org/ns/cargo#")
//                .subject(idProvider.createInternalIri().iri())
//                .add(RDF.TYPE, Values.iri("api:Notification"))
//                .add("api:hasEventType", Values.iri("https://1r.example.com/logistics-objects/1a8ded38-1804-467c-a369-81a411416b7c"))
//                .add("api:hasEventType", Values.iri("api:LOGISTICS_OBJECT_CREATED"))
//                .add("api:isTriggeredBy", Values.iri("https://1r.example.com/action-requests/599fea49-7287-42af-b441-1fa618d2aaed"))
//                .add("api:hasLogisticsObjectType", Values.iri("https://onerecord.iata.org/ns/cargo#Piece"))
//                .build();

//        notificationClientService.sendNotification(model);
    }
}
