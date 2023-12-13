package com.ontomesh.onerecord.server.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public interface EventProducer {

    void send(Model eventModel, IRI eventId, IRI tenantId);
}
