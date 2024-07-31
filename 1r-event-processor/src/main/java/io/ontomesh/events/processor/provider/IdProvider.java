package io.ontomesh.events.processor.provider;

import io.ontomesh.events.processor.config.LoIdConfig;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;

@Component
public class IdProvider {

    private final LoIdConfig loIdConfig;

    public IdProvider(LoIdConfig loIdConfig) {
        this.loIdConfig = loIdConfig;
    }

    public IRI createInternalIri() {
        return OntoMeshId.fromIri(Values.iri(STR."\{loIdConfig.getInternalIriScheme()}:\{UUID.randomUUID()}")).iri();
    }

    record OntoMeshId(IRI iri, URI uri) {
        public static OntoMeshId fromIri(IRI iri) {
            return new OntoMeshId(iri, URI.create(iri.stringValue()));
        }

    }
}
