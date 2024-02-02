package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.util.RDFUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import java.net.URI;

public record LogisticsObject(IRI iri, IRI type, Model model) {

    @Override
    public String toString() {
        return "LogisticsObject{" +
                "iri=" + iri +
                ", type=" + type +
                '}';
    }

    public URI uri() {
        if (iri != null)
            return URI.create(iri.toString());
        return null;
    }

    public void print() {
        // printing using default format
        RDFUtil.print(this);
    }
}
