package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.enums.EventStatus;
import com.ontomesh.onerecord.server.api.enums.NotificationEventType;
import com.ontomesh.onerecord.server.common.OntoMesh;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LogisticsEventBuilder {

    /**
     * convert event loId to rdf model.
     *
     * @param id                    event IRI
     * @param loId                  Logistics Object IRI
     * @param notificationEventType Notification Event Type
     * @param eventStatus           Event Status
     * @param triggeredBy           Triggered By
     * @return rdf model.
     */
    public Model createLogisticsEvent(IRI id, IRI loId, NotificationEventType notificationEventType,
                                      EventStatus eventStatus, Optional<IRI> triggeredBy) {

        ModelBuilder builder = new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(id)
                .add(RDF.TYPE, CARGO.LogisticsEvent)
                .add(API.hasLogisticsObject, loId)
                .add(OntoMesh.hasEventType, notificationEventType.label())
                .add(OntoMesh.hasEventStatus, eventStatus.label());

        if (triggeredBy.isPresent()) {
            builder.add(OntoMesh.hasTriggeredBy, triggeredBy);
        }

        return builder.build();
    }
}
