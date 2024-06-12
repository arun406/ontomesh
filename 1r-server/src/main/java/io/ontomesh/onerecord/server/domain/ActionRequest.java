package io.ontomesh.onerecord.server.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.ontomesh.onerecord.server.api.ActionRequestSerializer;
import org.eclipse.rdf4j.model.IRI;

import java.time.LocalDateTime;

@JsonSerialize(using = ActionRequestSerializer.class)
public record ActionRequest(@JsonProperty("@id") IRI iri, IRI status, LocalDateTime requestedAt) {
}
