package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.exception.RevisionMissingException;
import io.ontomesh.onerecord.server.util.RDFUtil;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;

import java.net.URI;

/**
 * Main Domain object to represent the Logistics Object.
 *
 * @param iri
 * @param type
 * @param model
 */
public record LogisticsObject(IRI iri, IRI type, Model model) {

    @Override
    public String toString() {
        return "LogisticsObject{" +
                "iri=" + iri +
                ", type=" + type +
                '}';
    }

    public URI uri() {
        return URI.create(iri.toString());
    }

    public void print() {
        // printing using default format
        RDFUtil.print(this);
    }

    public void print(RDFFormat format) {
        RDFUtil.print(this, format);
    }

    public IRI findIRI(IRI predicate, boolean raiseException) {
        return RDFUtil.getObjectIRI(this.model, this.iri, predicate, raiseException);
    }

    public Value findValue(IRI predicate) {
        return this.model.filter(this.iri, predicate, null).stream()
                .findFirst()
                .map(Statement::getObject)
                .orElse(null);
    }

    /**
     * returns the logistics object revision from the logistics model.
     *
     * @return logistics object revision number.
     */
    public int revision() {
        return Models.objectLiteral(this.model().filter(iri, API.hasRevision, null))
                .orElseThrow(() -> new RevisionMissingException(iri))
                .intValue();

    }

    public void updateRevision(Literal newRevision) {
        model().remove(iri, API.hasRevision, null);
        model().add(iri, API.hasRevision, newRevision);
    }
}
