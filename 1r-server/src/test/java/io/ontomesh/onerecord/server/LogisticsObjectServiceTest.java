package io.ontomesh.onerecord.server;

import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class LogisticsObjectServiceTest {

    @Autowired
    LogisticsObjectRepository repository;


    @Test
    public void updateRevision() {
        IRI logisticsObjectIRI = Values.iri("http://server.ontomesh.io:8085/logistics-objects/d3aaf89a-b0d7-4663-a694-d860c58dfea5");

        Literal currentRevision = Values.getValueFactory().createLiteral(11);
        Literal nextRevision = Values.getValueFactory().createLiteral(12);

        Literal literal = repository.incrementLogisticsObjectVersion(logisticsObjectIRI, currentRevision);
        Assertions.assertEquals(literal.intValue(), nextRevision.intValue());

    }

    @Test
    public void listLogisticsObjectsByType() {
        List<IRI> iris = repository.listAllLogisticsObjects(Values.iri("https://onerecord.iata.org/ns/cargo#Shipment"), 20, 0);
    }
}
