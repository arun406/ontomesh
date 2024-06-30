package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.exception.RevisionMissingException;
import io.ontomesh.onerecord.server.util.RDFUtil;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

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
        this(iri, model, Models.objectLiteral(model.filter(iri, API.hasRevision, null)).orElse(Values.literal(1)));
    }

    // validate the parameters and raise the exception if properties are not valid.
    // define an interface
    @Override
    public String toString() {
        return "LogisticsObject{" +
                "iri=" + iri +
                '}';
    }

    @Override
    public URI uri() {
        return URI.create(iri.toString());
    }

    @Override
    public void print() {
        // printing using default format
        RDFUtil.print(this.model());
    }

    public void print(RDFFormat format) {
        RDFUtil.print(this.model(), format);
    }

    @Override
    public IRI findIRI(IRI predicate, boolean raiseException) {
        return RDFUtil.getObjectIRI(this.model, this.iri, predicate, raiseException);
    }

    public Value findValue(IRI predicate) {
        return this.model.filter(this.iri, predicate, null).stream()
                .findFirst()
                .map(Statement::getObject)
                .orElse(null);
    }

    @Override
    public boolean add(IRI predicate, Value value) {
        return model.add(this.iri, predicate, value);
    }

    @Override
    public boolean remove(IRI predicate, Value value) {
        return model.remove(this.iri, predicate, value);
    }

    /**
     * returns the logistics object revision from the logistics model.
     *
     * @return logistics object revision number.
     */
    public Literal revision() {
        if (revision != null) {
            return this.revision;
        }
        return Models.objectLiteral(this.model().filter(iri, API.hasRevision, null))
                .orElseThrow(() -> new RevisionMissingException(iri));

    }

    @Transactional
    public void updateRevision(int newRevision) {
        add(API.hasRevision, null);
        remove(API.hasRevision, Values.literal(newRevision));
    }

    public IRI getType() {
        return CARGO.LogisticsEvent;
    }

}
