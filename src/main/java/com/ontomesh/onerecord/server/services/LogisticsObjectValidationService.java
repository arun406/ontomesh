package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.LogisticsObjectURINotMatchedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LogisticsObjectValidationService {

    public void validate(LogisticsObject logisticsObject, IRI pathIRI) {
        Model model = logisticsObject.model();
        Optional<IRI> optionalIRI = Models.objectIRI(model.filter(logisticsObject.iri(), API.hasLogisticsObject, null));
        IRI loIRI = optionalIRI.orElseThrow(RuntimeException::new);
        if (!pathIRI.equals(loIRI)) {
            LogisticsObjectURINotMatchedException exception = new LogisticsObjectURINotMatchedException();
            exception.setIri(pathIRI);
            throw exception;
        }
    }
}
