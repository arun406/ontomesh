package io.ontomesh.onerecord.server.api.enums;

import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;

public enum RequestType {
    SubscriptionRequest(API.SubscriptionRequest),
    ChangeRequest(API.ChangeRequest),
    AccessDelegationRequest(API.AccessDelegation);

    private IRI iri;

    RequestType(IRI iri) {
        this.iri = iri;
    }

    public IRI getIri() {
        return this.iri;
    }
}
