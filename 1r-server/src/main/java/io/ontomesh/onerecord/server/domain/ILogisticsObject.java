package io.ontomesh.onerecord.server.domain;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;

public interface ILogisticsObject {
    Literal revision();

    void print();

    URI uri();

    IRI findIRI(IRI predicate, boolean raiseException);

    Value findValue(IRI predicate);

    boolean add(IRI var1, Value var2);

    boolean remove(IRI var1, Value var2);
}
