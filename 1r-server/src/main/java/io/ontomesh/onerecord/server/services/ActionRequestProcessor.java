package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.beans.factory.annotation.Autowired;

public interface ActionRequestProcessor {
    void process(LogisticsObject actionRequest);

    IRI requestType();

    @Autowired
    default void register(ActionRequestProcessExecutor processor) {
        processor.registerProcessor(requestType(), this);
    }
}
