package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.exception.LogisticsObjectURINotMatchedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

@Service
public class LogisticsObjectValidationService {

    public void validate(IRI loID, Model model, IRI changeReferenceIRI) {

        IRI loIRI = Models
                .objectIRI(model.filter(changeReferenceIRI, API.hasLogisticsObject, null))
                .orElseThrow(RuntimeException::new);
//        IRI loIRI = optionalIRI.orElseThrow(RuntimeException::new);
        /**
         * The ONE Record server MUST check that the property referencesLogisticsObject matches the URI/endpoint
         * used for the PATCH request.
         */
        if (!loID.equals(loIRI)) {
            LogisticsObjectURINotMatchedException exception = new LogisticsObjectURINotMatchedException();
            exception.setIri(loID);
            throw exception;
        }
    }
}
