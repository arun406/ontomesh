package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.config.LoIdConfig;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Slf4j
@Component
public class IdProvider {

    private final String rootPath;
    private final LoIdConfig loIdConfig;

    public IdProvider(@Value("${server.servlet.context-path:/}") String rootPath, LoIdConfig loIdConfig) {
        this.rootPath = rootPath;
        this.loIdConfig = loIdConfig;
    }

    public OntoMeshId createInternalIri() {
        return OntoMeshId.fromIri(Values.iri(loIdConfig.getInternalIriScheme() + ":" + UUID.randomUUID()));
    }

    public OntoMeshId createUniqueLoUri() {
        return OntoMeshId.fromUri(getLoUriBase().path(UUID.randomUUID().toString()).build().toUri());
    }

    public OntoMeshId getUriForLoId(String uuid) {
        return OntoMeshId.fromUri(getLoUriBase().path(uuid).build().toUri());
    }

    private UriComponentsBuilder getLoUriBase() {
        return baseUri()
                .path("/logistics-objects/");
    }

    private UriComponentsBuilder baseUri() {
        return UriComponentsBuilder.newInstance().scheme(loIdConfig.getScheme())
                .host(loIdConfig.getHost())
                .port(loIdConfig.getPort());
    }

    public IRI getLogisticsObjectBaseIRI() {
        return Values.iri(this.getLoUriBase().build().toString());
    }
}
