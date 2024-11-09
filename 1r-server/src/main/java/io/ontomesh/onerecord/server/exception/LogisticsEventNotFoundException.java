package io.ontomesh.onerecord.server.exception;

import lombok.Getter;
import org.eclipse.rdf4j.model.IRI;


@Getter
public class LogisticsEventNotFoundException extends RuntimeException {
    private IRI iri;

    public LogisticsEventNotFoundException(IRI iri) {
        super(String.format("Logistics event not found with IRI %s", iri));
        this.iri = iri;
    }

    public LogisticsEventNotFoundException(IRI iri, String s) {
        super(s);
        this.iri = iri;
    }
}
