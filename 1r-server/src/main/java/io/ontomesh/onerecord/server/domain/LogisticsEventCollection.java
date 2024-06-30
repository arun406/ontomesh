package io.ontomesh.onerecord.server.domain;

import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public record LogisticsEventCollection(IRI id, List<LogisticsEvent> items) {

    public int getTotalItems() {
        return this.items.size();
    }
}
