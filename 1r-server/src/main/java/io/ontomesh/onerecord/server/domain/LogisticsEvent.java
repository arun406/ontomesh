package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.util.RDFUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;

/**
 * Main Domain object to represent the Logistics Object.
 *
 * @param iri
 * @param model
 */
public record LogisticsEvent(IRI iri, Model model, Literal revision) implements ILogisticsObject {

    // define constructor
    public LogisticsEvent(IRI iri, Model model, Literal revision) {
        if (iri == null) {
            throw new IllegalArgumentException("Invalid IRI");
        }
        if (model.isEmpty()) {
            throw new IllegalArgumentException("Invalid Or empty Model.");
        }
        if (revision != null && revision.intValue() < 0) {
            throw new IllegalArgumentException("Invalid revision.");
        }

        this.iri = iri;
        this.model = model;
        this.revision = revision;
        add(API.hasRevision, revision);
    }

    // define constructor without type and derive it from model.
    public LogisticsEvent(IRI iri, Model model) {
        this(iri, model, Models.objectLiteral(model.filter(iri, API.hasRevision, null))
                .orElse(Values.literal(1)));
    }

    @Override
    public String toString() {
        return String.format("LogisticsObject{iri=%s{}}", iri);
    }


    public void print(RDFFormat format) {
        RDFUtil.print(this.model(), format);
    }

    @Override
    public IRI type() {
        return CARGO.LogisticsEvent;
    }

    public String getLogisticsEventCode() {
        Model model = model().filter(iri(), CARGO.eventCode, null);
        IRI objectIRI = Models.objectIRI(model).orElse(null);
        if (objectIRI != null) {
            Literal literal = Models.objectLiteral(model().filter(objectIRI, CARGO.code, null)).orElse(null);
            if (literal != null) {
                return literal.stringValue();
            }
        }
        return null;
    }
}
