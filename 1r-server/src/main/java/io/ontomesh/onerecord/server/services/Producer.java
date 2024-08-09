package io.ontomesh.onerecord.server.services;

import org.eclipse.rdf4j.model.IRI;
import org.springframework.kafka.support.SendResult;

import java.util.function.BiConsumer;

public interface Producer {

    void send(String key, String message, IRI tenantId,
              BiConsumer<? super SendResult<String, String>, ? super Throwable> action);
}
