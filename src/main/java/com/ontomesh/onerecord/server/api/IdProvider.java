package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.config.LoIdConfig;
import com.ontomesh.onerecord.server.services.ServerInformationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Slf4j
@Component
public class IdProvider {

    private final String rootPath;
    private final LoIdConfig loIdConfig;
    private final ServerInformationService serverInformationService;

    public IdProvider(@Value("${server.servlet.context-path:/}") String rootPath, LoIdConfig loIdConfig,
                      HttpServletRequest httpServletRequest, ServerInformationService serverInformationService) {
        this.rootPath = rootPath;
        this.loIdConfig = loIdConfig;
        this.serverInformationService = serverInformationService;
    }

    public OntoMeshId createInternalIri() {
        return OntoMeshId.fromIri(Values.iri(loIdConfig.getInternalIriScheme() + ":" + UUID.randomUUID()));
    }

    public OntoMeshId createUniqueLoUri() {
        return OntoMeshId.fromUri(getLoUriBase().path(UUID.randomUUID().toString()).build().toUri());
    }

    public OntoMeshId createUniqueActionRequestUri() {
        return OntoMeshId.fromUri(getActionRequestUriBase().path(UUID.randomUUID().toString()).build().toUri());
    }

    public OntoMeshId createUniqueLogisticsEventUri(URI path) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().uri(path).path("/");
        return OntoMeshId.fromUri(builder.path(UUID.randomUUID().toString()).build().toUri());
    }

    public OntoMeshId createUniqueLogisticsEvent(IRI loId) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .uri(URI.create(loId.stringValue())).path("/");
        return OntoMeshId.fromUri(builder.path("/logistics-events/")
                .path(UUID.randomUUID().toString()).build().toUri());
    }

    public OntoMeshId createUniqueLogisticsEventUri(String uuid) {
        UriComponentsBuilder builder = getEventRequestUriBase(uuid);
        return OntoMeshId.fromUri(builder.path(UUID.randomUUID().toString()).build().toUri());
    }

    public OntoMeshId getUriForLoId(String uuid) {
        return OntoMeshId.fromUri(getLoUriBase().path(uuid).build().toUri());
    }

    public OntoMeshId getUriForActionRequestId(String uuid) {
        return OntoMeshId.fromUri(getActionRequestUriBase().path(uuid).build().toUri());
    }

    public OntoMeshId getUriForLogisticsEventId(String uuid1, String uuid2) {
        return OntoMeshId.fromUri(getEventRequestUriBase(uuid1).path(uuid2).build().toUri());
    }

    private UriComponentsBuilder getLoUriBase() {
        return baseUri()
                .path("/logistics-objects/");
    }

    private UriComponentsBuilder getActionRequestUriBase() {
        return baseUri()
                .path("/action-requests/");
    }

    private UriComponentsBuilder getEventRequestUriBase(String uuid) {
        return getLoUriBase().path(uuid)
                .path("/logistics-events/");
    }

    private UriComponentsBuilder baseUri() {
        return UriComponentsBuilder.newInstance().scheme(loIdConfig.getScheme())
                .host(loIdConfig.getHost())
                .port(loIdConfig.getPort());
    }

    public IRI getLogisticsObjectBaseIRI() {
        return Values.iri(this.getLoUriBase().build().toString());
    }


    public IRI tenant() {
        return this.serverInformationService.getTenant();
    }
}
