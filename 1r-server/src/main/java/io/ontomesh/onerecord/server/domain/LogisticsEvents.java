package io.ontomesh.onerecord.server.domain;

import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.util.RDFUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.net.URI;

public record LogisticsEvents(IRI iri, Model model) implements ILogisticsObject {

    public int getTotalItems() {
        return Literals.getIntValue(findValue(OntoMesh.hasTotalItems), 0);
    }

    public IRI type() {
        return OntoMesh.Collection;
    }

}
