package com.ontomesh.onerecord.server.api.enums;

import com.ontomesh.onerecord.server.common.OntoMesh;
import org.eclipse.rdf4j.model.IRI;

public enum EventStatus implements Labeled {
    NEW(OntoMesh.hasEventStatusNew),
    FAILED(OntoMesh.hasEventStatusFailed),
    PENDING(OntoMesh.hasEventStatusPending),
    PROCESSED(OntoMesh.hasEventStatusProcessed),
    PUBLISHED(OntoMesh.hasEventStatusPublished),
    CONSUMED(OntoMesh.hasEventStatusConsumed),
    ;

    public final IRI hasEventStatus;

    EventStatus(IRI hasEventStatus) {
        this.hasEventStatus = hasEventStatus;
    }

    @Override
    public IRI label() {
        return hasEventStatus;
    }
}
