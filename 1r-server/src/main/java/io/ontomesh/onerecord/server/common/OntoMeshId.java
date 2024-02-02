package io.ontomesh.onerecord.server.common;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
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

    public static boolean isInternalIri(Statement statement) {
        if (statement != null && statement.getObject() != null && statement.getObject().isIRI()) {
            IRI iri = (IRI) statement.getObject();
            return iri.getNamespace().equals(ONTOMESH_NAMESPACE);
        }
        return false;
    }

    public static boolean isOneRecordIri(IRI iri) {
        return iri.getNamespace().startsWith(CARGO.NAMESPACE)
                || iri.getNamespace().startsWith(API.NAMESPACE);
    }


    public static boolean isValidIri(Statement statement, IRI logisticsObjectBaseIRI) {
        if (statement != null && statement.getObject() != null && statement.getObject().isIRI()) {
            IRI iri = (IRI) statement.getObject();
            if (statement.getPredicate().isIRI()) {
                log.info("statement type: {}", statement.getPredicate().stringValue());
            }
            return (OntoMeshId.isInternalIri(iri)) || OntoMeshId.isOneRecordIri(iri)
                    || iri.getNamespace().equals(logisticsObjectBaseIRI.stringValue());
        }
        return false;
    }

    public static boolean isValidIri(Statement statement) {

        if (statement != null && statement.getObject() != null && statement.getObject().isIRI()) {
            IRI iri = (IRI) statement.getObject();
            return OntoMeshId.isInternalIri(iri) || OntoMeshId.isOneRecordIri(iri);
        }
        return false;
    }

    public static boolean findParentOnly(IRI iri) {
        return false;
    }

    public static boolean findParentOnly(Statement statement) {
        return false;
    }

    public static boolean isNotLogisticsObject() {
//        return !iri.getNamespace().equals(logisticsObjectBaseIRI.stringValue());
        return true;
    }

    public static boolean isActionRequest(IRI iri) {
        IRI type = null;

        return type.equals(API.Change) || type.equals(API.Subscription) || type.equals(API.AccessDelegation);
    }

    public static boolean isInternalIri(IRI iri) {
        if (iri == null) {
            return false;
        }
        return iri.getNamespace().equals(ONTOMESH_NAMESPACE);
    }
}
