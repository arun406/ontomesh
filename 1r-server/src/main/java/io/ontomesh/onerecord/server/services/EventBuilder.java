package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.common.OntoMesh;
import lombok.Setter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;

import java.time.LocalDateTime;

@Setter
public class EventBuilder {

    private IRI id;
    private IRI loId;
    private String eventName;
    private boolean internal;
    private IRI company;
    private IRI eventCodeIRI;
    private IRI eventTimeTypeIRI;

    public EventBuilder(IRI id, IRI loId, IRI loType, String eventName,
                        boolean internal, IRI company, IRI eventCodeIRI, IRI eventTimeTypeIRI) {
        this.id = id;
        this.loId = loId;
        this.eventName = eventName;
        this.internal = internal;
        this.company = company;
        this.eventCodeIRI = eventCodeIRI;
        this.eventTimeTypeIRI = eventTimeTypeIRI;
    }

    /**
     * convert event loId to rdf model.
     *
     * @return rdf model.
     */
    public Model build() {


        ModelBuilder builder = new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(id)
                .add(RDF.TYPE, CARGO.LogisticsEvent)
                .add(CARGO.creationDate, LocalDateTime.now())
                .add(CARGO.eventDate, LocalDateTime.now())//TODO : revisit
//                .add(CARGO.eventCode, eventType.label())
                .add(CARGO.eventName, eventName)
                .add(CARGO.eventTimeType, eventTimeTypeIRI)
                .add(CARGO.partialEventIndicator, false)
                .add(CARGO.eventFor, loId)
//                .subject(eventFor)
//                .add(RDF.TYPE, loType)
//                .subject(loId)
//                .add(RDF.TYPE, CARGO.Piece)
//                .subject(id)
                .add(CARGO.recordingOrganization, company)
//                .add(company, RDF.TYPE, CARGO.Company)
//                .add(API.hasLogisticsObject, loId)
                .add(OntoMesh.isInternal, Values.literal(internal));
//                .add(OntoMesh.hasEventType, eventType.label())
//                .add(OntoMesh.hasEventStatus, eventStatus.label());

//        if (triggeredBy.isPresent()) {
//            builder.add(OntoMesh.hasTriggeredBy, triggeredBy);
//        }

        Model model = builder.build();
        model.add(id, CARGO.eventCode, eventCodeIRI);


        return model;
    }
}
