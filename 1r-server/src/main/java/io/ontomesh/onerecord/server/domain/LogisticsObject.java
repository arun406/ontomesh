package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.exception.RevisionMissingException;
import io.ontomesh.onerecord.server.util.RDFUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main Domain object to represent the Logistics Object.
 *
 * @param iri
 * @param type
 * @param model
 */
public record LogisticsObject(IRI iri, IRI type, Model model, Literal revision) implements IVersionedLogisticsObject {

    // define constructor
    public LogisticsObject(IRI iri, IRI type, Model model, Literal revision) {
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
        this.type = type;
        this.revision = revision;
        add(API.hasRevision, revision);
    }

    // define constructor without type and derive it from model.
    public LogisticsObject(IRI iri, Model model) {
        this(iri, RDFUtil.getType(model, iri), model);
    }

    public LogisticsObject(IRI iri, IRI type, Model model) {
        this(iri, type, model, Models.objectLiteral(model.filter(iri, API.hasRevision, null)).orElse(Values.literal(1)));
    }

    @Override
    public String toString() {
//        return STR."LogisticsObject{iri=\{iri}, type=\{type}\{'}'}";
        return String.format("LogisticsObject{iri=%s, type=%s{}}", iri, type);
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
}
