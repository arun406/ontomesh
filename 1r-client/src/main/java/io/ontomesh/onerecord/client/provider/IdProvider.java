package io.ontomesh.onerecord.client.provider;

import io.ontomesh.onerecord.client.OntoMeshId;
import io.ontomesh.onerecord.client.config.LoIdConfig;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdProvider {

    private final LoIdConfig loIdConfig;

    public IdProvider(LoIdConfig loIdConfig) {
        this.loIdConfig = loIdConfig;
    }

    public OntoMeshId createInternalIri() {
        return OntoMeshId.fromIri(Values.iri(loIdConfig.getInternalIriScheme() + ":" + UUID.randomUUID()));
    }
}
