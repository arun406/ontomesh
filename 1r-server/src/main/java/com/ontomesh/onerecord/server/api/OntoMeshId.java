package com.ontomesh.onerecord.server.api;

import lombok.Data;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import java.net.URI;

@Data
public class OntoMeshId {
    private final IRI iri;
    private final URI uri;

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
        return iri.getNamespace().equals("ontomesh:");
    }
}
