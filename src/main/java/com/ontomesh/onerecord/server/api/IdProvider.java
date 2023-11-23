package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.config.LoIdConfig;
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

    public IdProvider(@Value("${server.servlet.context-path:/}") String rootPath, LoIdConfig loIdConfig,
                      HttpServletRequest httpServletRequest) {
        this.rootPath = rootPath;
        this.loIdConfig = loIdConfig;
        this.httpServletRequest = httpServletRequest;
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

    private final HttpServletRequest httpServletRequest;


    private String getURL() {
        return httpServletRequest.getRequestURL().toString();
    }

    public OntoMeshId createUniqueUri() {
        String url = getURL();
        log.debug("URL : {} ", url);
        boolean isLogisticsEvent = url.contains("/logistics-events");
        if (isLogisticsEvent) {
            return createUniqueLogisticsEventUri(URI.create(url));
        } else {
            return createUniqueLoUri();
        }
    }
}
