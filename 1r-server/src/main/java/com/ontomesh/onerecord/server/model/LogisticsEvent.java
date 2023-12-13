package com.ontomesh.onerecord.server.model;

import com.ontomesh.onerecord.server.api.enums.EventStatus;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import java.util.Optional;

public record LogisticsEvent(
        IRI id,
        IRI loId,
        IRI eventType,
        EventStatus status,
        Optional<IRI> triggeredBy,
        Optional<Model> change) {
}
