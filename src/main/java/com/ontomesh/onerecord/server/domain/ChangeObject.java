package com.ontomesh.onerecord.server.domain;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public record ChangeObject(IRI changeId, IRI loId, IRI loType, Model changeModel) {
}
