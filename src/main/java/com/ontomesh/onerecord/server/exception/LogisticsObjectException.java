package com.ontomesh.onerecord.server.exception;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.rdf4j.model.IRI;

@Getter
public class LogisticsObjectException extends RuntimeException {

    @Setter
    private IRI loId;

    public LogisticsObjectException(String message) {
        super(message);
    }

    public LogisticsObjectException(IRI loId, String message) {
        super(message);
        this.loId = loId;
    }
}
