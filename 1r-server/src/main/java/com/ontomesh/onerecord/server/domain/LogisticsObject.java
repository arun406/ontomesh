package com.ontomesh.onerecord.server.domain;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public record LogisticsObject(IRI iri, IRI type, Model model) {
}
