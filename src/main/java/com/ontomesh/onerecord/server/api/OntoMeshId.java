package com.ontomesh.onerecord.server.api;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;

import java.net.URI;

@Slf4j
public record OntoMeshId(IRI iri, URI uri) {

    public static final String ONTOMESH_NAMESPACE = "ontomesh:";

    public OntoMeshId(IRI iri, URI uri) {
        this.iri = iri;
        this.uri = uri;
    }

    public static OntoMeshId fromUri(URI uri) {
        return new OntoMeshId(Values.iri(uri.toString()), uri);
    }

    public static OntoMeshId fromIri(IRI iri) {
        return new OntoMeshId(iri, URI.create(iri.stringValue()));
    }

    public static boolean isInternalIri(IRI iri) {
        return iri.getNamespace().equals(ONTOMESH_NAMESPACE);
    }

    public static boolean isOneRecordIri(IRI iri) {
        return iri.getNamespace().startsWith(CARGO.NAMESPACE)
                || iri.getNamespace().startsWith(API.NAMESPACE);
    }
}
