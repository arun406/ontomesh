package io.ontomesh.core;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import java.net.URI;

@Slf4j
public record OntoMeshId(IRI iri, URI uri) {
    public static final String ONTOMESH_NAMESPACE = "ontomesh:";

    public static boolean isInternalIri(IRI iri) {
        return iri.getNamespace().equals(ONTOMESH_NAMESPACE);
    }

    public static OntoMeshId fromUri(URI uri) {
        return new OntoMeshId(Values.iri(uri.toString()), uri);
    }

    public static OntoMeshId fromIri(IRI iri) {
        return new OntoMeshId(iri, URI.create(iri.stringValue()));
    }

}
