package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.common.OntoMesh;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Literals;

public record LogisticsEvents(IRI iri, Model model) implements ILogisticsObject {

    public int getTotalItems() {
        return Literals.getIntValue(findValue(OntoMesh.hasTotalItems), 0);
    }

    public IRI type() {
        return OntoMesh.Collection;
    }

}
