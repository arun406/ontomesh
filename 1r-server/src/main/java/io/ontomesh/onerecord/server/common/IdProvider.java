package io.ontomesh.onerecord.server.common;

import io.ontomesh.core.BaseIdProvider;
import io.ontomesh.core.properties.ServerProperties;
import io.ontomesh.onerecord.server.util.URIUtil;
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
public class IdProvider extends BaseIdProvider {

    private final String rootPath;
    private final ServerProperties serverProperties;

    public IdProvider(@Value("${server.servlet.context-path:/}") String rootPath, ServerProperties serverProperties,
                      HttpServletRequest httpServletRequest) {
        super(serverProperties.getInternalIriScheme());
        this.rootPath = rootPath;
        this.serverProperties = serverProperties;
    }

    private UriComponentsBuilder baseUri() {
        return UriComponentsBuilder.newInstance().uri(URIUtil.toURI(Values.iri(serverProperties.getUrl())));
    }

    private UriComponentsBuilder getActionRequestUriBase() {
        return baseUri()
                .path("/action-requests/");
    }

    private UriComponentsBuilder getEventRequestUriBase(String uuid) {
        return getLoUriBase().path(uuid)
                .path("/logistics-events/");
    }

    public OntoMeshId getLogisticsEventId(IRI logisticsObjectId) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .uri(URI.create(logisticsObjectId.stringValue())).path("/");
        return OntoMeshId.fromUri(builder.path("/logistics-events/").build().toUri());
    }

    private UriComponentsBuilder getLoUriBase() {
        return baseUri()
                .path("/logistics-objects/");
    }

    private UriComponentsBuilder getSubscriptionsUriBase() {
        return baseUri()
                .path("/subscriptions/");
    }

//    public OntoMeshId createInternalIri() {
//        return OntoMeshId.fromIri(Values.iri(STR."\{serverProperties.getInternalIriScheme()}:\{UUID.randomUUID()}"));
//    }

    public OntoMeshId createUniqueLoUri() {
        return OntoMeshId.fromUri(getLoUriBase().path(UUID.randomUUID().toString()).build().toUri());
    }

    public OntoMeshId createUniqueSubscriptionUri() {
        return OntoMeshId.fromUri(getSubscriptionsUriBase().path(UUID.randomUUID().toString()).build().toUri());
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

    public OntoMeshId createUniqueNotificationEvent(IRI loId) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .uri(URI.create(loId.stringValue())).path("/");
        return OntoMeshId.fromUri(builder.path("/notification-events/")
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

    public IRI getLogisticsObjectBaseIRI() {
        return Values.iri(this.getLoUriBase().build().toString());
    }

}
