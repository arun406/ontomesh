package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.util.RDFUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

/**
 * @author arun
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ActionRequestProcessorImpl implements ActionRequestProcessor {

    private final LogisticsObjectService logisticsObjectService;
    private final SnapshotService snapshotService;

    /**
     * process the action request.
     *
     * @param actionRequest Action Request.
     */
    @Override
    public void process(LogisticsObject actionRequest) {
        log.debug("Processing action request");
        Model actionRequestModel = actionRequest.model();
        IRI actionRequestType = actionRequest.type();
        if (actionRequestType.stringValue().equals(API.ChangeRequest.stringValue())) {
            processChangeRequest(actionRequestModel);
        }
    }

    private void processChangeRequest(Model actionRequestModel) {
        
        Model changeModel = RDFUtil.getSubModel(actionRequestModel, API.hasChange);
        IRI changeIRI = RDFUtil.getRootIri(changeModel);
        IRI changeType = RDFUtil.getType(changeModel, changeIRI);
        log.debug("Change IRI {} and Type {} ", changeIRI, changeType);

        LogisticsObject change = new LogisticsObject(changeIRI, changeType, changeModel);
        RDFUtil.print(change, RDFFormat.TURTLE);

        IRI logisticsObjectIRI = Models
                .objectIRI(changeModel.filter(changeIRI, API.hasLogisticsObject, null))
                .orElse(null);

        log.debug("Logistics Object IRI {}", logisticsObjectIRI);
        if (logisticsObjectIRI != null) {
            LogisticsObject logisticsObject = logisticsObjectService.findLogisticsObject(logisticsObjectIRI);
            RDFUtil.print(logisticsObject.model(), RDFFormat.TURTLE);
            logisticsObjectService.processActionRequest(change, logisticsObject);
            LogisticsObject snapshot = snapshotService.createSnapshot(logisticsObject);
            log.debug("Snapshot with {} is created successfully.", snapshot.iri());
        }
    }
}
