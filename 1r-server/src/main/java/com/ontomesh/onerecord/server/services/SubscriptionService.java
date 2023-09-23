package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.repository.LoRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static com.ontomesh.onerecord.server.api.SemanticWebMediaType.*;

@Service
public class SubscriptionService {
    private final LoRepository repository;
    private final IdProvider idProvider;

    @Value("${company.identifier:MyAirline}")
    String companyIdentifier;

    public SubscriptionService(LoRepository repository, IdProvider idProvider) {
        this.repository = repository;
        this.idProvider = idProvider;
    }

    /**
     * @param logisticsObject
     */
    public void createSubscriptionRequest(LogisticsObject logisticsObject) {
        Model model = logisticsObject.model();
        // override the type to SubscriptionRequest
        model.add(logisticsObject.iri(), RDF.TYPE, API.SubscriptionRequest);
        model.add(logisticsObject.iri(), API.isRequestedAt,
                Values.literal(Instant.now().toString(), CoreDatatype.XSD.DATETIME));
        this.repository.persist(logisticsObject.iri(), model);
    }


    /**
     * @param topic
     * @param topicType
     * @return
     */
    public LogisticsObject createSubscriptionProposalResponse(String topic, String topicType) {

        IRI subscriber = Values.iri(companyIdentifier);
        IRI iri = idProvider.createUniqueLoUri().getIri();
        ModelBuilder modelBuilder = new ModelBuilder()
                .subject(iri)
                .add(RDF.TYPE, API.Subscription)
                .add(API.hasSubscriber, subscriber)
                .add(API.hasTopic, topic)
                .add(API.hasTopicType, topicType)
                .add(API.subscribeToLogisticsEvents, Values.literal(true)) //TODO: remove the hard coding.
                .add(API.expiresAt, Values.literal(Instant.now().toString(), CoreDatatype.XSD.DATETIME))
                .add(API.sendLogisticsObjectBody, Values.literal(true));//TODO: remove the hard coding.
        contentTypes().forEach(contentType -> modelBuilder.add(API.hasSupportedContentType, Values.literal(contentType)));

        Model model = modelBuilder.build();
        return new LogisticsObject(iri, model);
    }

    public static List<String> contentTypes() {
        return List.of(APPLICATION_JSON_LD_VALUE,
                APPLICATION_TURTLE_VALUE,
                TEXT_TURTLE_VALUE);
    }
}
