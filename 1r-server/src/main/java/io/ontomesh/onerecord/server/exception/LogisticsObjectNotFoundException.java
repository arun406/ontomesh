package io.ontomesh.onerecord.server.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.rdf4j.model.IRI;

import java.io.Serial;

@Getter
@NoArgsConstructor
public class LogisticsObjectNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    @Setter
    private IRI iri;

    public LogisticsObjectNotFoundException(String s) {
        super(s);
    }

    public LogisticsObjectNotFoundException(IRI iri) {
        this.iri = iri;
    }

    public LogisticsObjectNotFoundException(IRI iri, String message) {
        super(message);
        this.iri = iri;
    }
}
