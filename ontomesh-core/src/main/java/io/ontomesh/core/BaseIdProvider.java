package io.ontomesh.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import java.util.UUID;

public class BaseIdProvider {

    private final String internalIriScheme;

    public BaseIdProvider(String internalIriScheme) {
        this.internalIriScheme = internalIriScheme;
    }

    public IRI createInternalIri() {
        return Values.iri(internalIriScheme + ":" + UUID.randomUUID());
    }
}
