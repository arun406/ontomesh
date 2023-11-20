package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.common.OntoMesh;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.LinkedObjectNotMatchException;
import com.ontomesh.onerecord.server.exception.LinkedObjectReferenceMissingException;
import com.ontomesh.onerecord.server.exception.LogisticsEventNotFoundException;
import com.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class LogisticsEventService extends AbstractLogisticsObjectService {
    private final LogisticsObjectService logisticsObjectService;

    protected LogisticsEventService(LogisticsObjectRepository repository,
                                    IdProvider idProvider, LogisticsObjectService logisticsObjectService) {
        super(repository, idProvider);
        this.logisticsObjectService = logisticsObjectService;
    }

    public IRI saveLogisticsEvent(IRI logisticsObjectIRI, LogisticsObject event) {
        validateLogisticsObject(logisticsObjectIRI, event);

        // save the model.
        this.repository.persist(event.model());
        this.logisticsObjectService.addEvent(logisticsObjectIRI, event);
        return event.iri();
    }

    public LogisticsObject getLogisticsEvent(IRI logisticEventIRI) {

        Optional<LogisticsObject> logisticsObjectOptional = this.repository
                .findLoById(logisticEventIRI, OntoMeshId::isInternalIri);
        if (logisticsObjectOptional.isEmpty()) {
            throw new LogisticsEventNotFoundException("Logistics events not found.");
        }
        return logisticsObjectOptional.get();
    }

    public LogisticsObject getLogisticsEventByLogisticsObject(IRI logisticsObjectIRI, URI id, String eventType) {

        // validate logistics object.
        ModelBuilder builder = new ModelBuilder().setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(id.toString())
                .add(RDF.TYPE, OntoMesh.Collection);
        RepositoryResult<Statement> repositoryResult = this.repository
                .getStatements(logisticsObjectIRI, OntoMesh.hasLogisticsEvents, null);
        List<Statement> statementList = repositoryResult.stream().toList();
        int counter = 0;
        for (Statement statement : statementList) {
            if (statement.getObject().isIRI()) {
                log.debug("Logistics Event IRI {}", statement.getObject());
                LogisticsObject logisticsEvent = getLogisticsEvent((IRI) statement.getObject());
                if (StringUtils.hasText(eventType) &&
                        !getLogisticsEventType(logisticsEvent.iri(), logisticsEvent.model(), eventType)) {
                    continue;
                }
                counter++;
                builder.add(OntoMesh.hasItems, (Resource) statement.getObject());
                builder.subject((Resource) statement.getObject());
                for (Statement eventStatement : logisticsEvent.model()) {
                    builder.add(eventStatement.getSubject(), eventStatement.getPredicate(), eventStatement.getObject());
                }
                builder.subject(id.toString());
            }
        }
        builder.add(id.toString(), OntoMesh.hasTotalItems, counter);
        Model model = builder.build();
        return new LogisticsObject(Values.iri(id.toString()), OntoMesh.Collection, model);
    }

    private boolean getLogisticsEventType(IRI iri, Model model, String eventType) {
        return !model.filter(iri, CARGO.eventCode, Values.literal(eventType)).isEmpty();
    }

    public Model getLogisticsEvents(String eventType) {
        return null;
    }

    public Model getLogisticsEvents() {
        return null;
    }

    private void validateLogisticsObject(IRI logisticsObjectIRI, LogisticsObject event) {
        validateLogisticsObject(logisticsObjectIRI);
        Model model = event.model().filter(event.iri(), CARGO.linkedObject, null);
        IRI linkedObjectId = Models.objectIRI(model).orElse(null);
        if (linkedObjectId == null) {
            throw new LinkedObjectReferenceMissingException("Linked Object Reference is missing.");
        }
        log.debug("linked object id: {} ", linkedObjectId);
        if (!linkedObjectId.stringValue().equals(logisticsObjectIRI.stringValue())) {
            throw new LinkedObjectNotMatchException("Logistics Object Id and Linked Object reference is not matching.");
        }
    }

    private boolean isValidIri(IRI iri) {
        return (OntoMeshId.isInternalIri(iri)) || OntoMeshId.isOneRecordIri(iri)
                || iri.getNamespace().equals(idProvider.getLogisticsObjectBaseIRI().stringValue());
    }

    private void validateLogisticsObject(IRI logisticsObjectIRI) {
        boolean exists = this.logisticsObjectService.isExists(logisticsObjectIRI);
        if (!exists) {
            throw new LogisticsObjectNotFoundException(logisticsObjectIRI, "Logistics Object with the specified URI could not be found in the database.");
        }
    }
}
