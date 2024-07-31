package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.util.RDFUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;

public interface ILogisticsObject {


    default void print() {
        RDFUtil.print(model());
    }

    default URI uri() {
        return URI.create(iri().toString());
    }

    default IRI findIRI(IRI predicate, boolean raiseException) {
        return RDFUtil.getObjectIRI(model(), iri(), predicate, raiseException);
    }

    default Value findValue(IRI predicate) {
        return model().filter(iri(), predicate, null).stream()
                .findFirst()
                .map(Statement::getObject)
                .orElse(null);
    }

    default boolean add(IRI predicate, Value value) {
        return model().add(this.iri(), predicate, value);
    }

    default boolean remove(IRI predicate, Value value) {
        return model().remove(this.iri(), predicate, value);
    }

    Model model();

    IRI iri();

    IRI type();

}
