package io.ontomesh.onerecord.server;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import io.ontomesh.core.rdf.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.services.LogisticsEventService;
import io.ontomesh.onerecord.server.services.ServerInformationService;
import io.ontomesh.onerecord.server.util.JsonldUtil;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.cargo.CARGO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.util.Map;

@Slf4j
@SpringBootTest
@EnableTransactionManagement
public class EventGeneratorTest {
    @Autowired
    private LogisticsEventService service;

    @Autowired
    private ServerInformationService serverInformationService;

    @Test
    public void testCreateEventModel() throws IOException {

        IdProvider idProvider = service.getIdProvider();
        IRI eventCodeIRI = idProvider.createInternalIri();
        IRI loId = Values.iri("http://1r.ontomesh.io/logistics-objects/a245e346-2598-496f-ab40-b50c782f0e0e");
        OntoMeshId eventId = idProvider.createUniqueLogisticsEvent(loId);
        IRI tenant = serverInformationService.getTenant();
        LogisticsEvent logisticsEvent = service.createEventModel(loId, NotificationEventType.LOGISTICS_OBJECT_CREATED, tenant);
        RDFUtil.print(logisticsEvent.model(), RDFFormat.JSONLD);

        ModelToStringConverter modelToStringConverter = new ModelToStringConverter(RDFFormat.JSONLD);
        String rdfString = modelToStringConverter.convert(logisticsEvent.model());
        rdfString = JsonldUtil.jsonLdToString(rdfString, true, CARGO.LogisticsEvent);
        Map<String, Map<String, String>> context = JsonldUtil.getContext();
        Object compact = JsonLdProcessor.compact(JsonUtils.fromString(rdfString), context,
                new JsonLdOptions());
        rdfString = JsonUtils.toPrettyString(compact);

        System.out.println(rdfString);

    }

}
