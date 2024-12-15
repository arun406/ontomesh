package io.ontomesh.onerecord.server.services.changerequests;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.actionrequests.ActionRequestProcessor;
import io.ontomesh.onerecord.server.services.logisticsobjects.LogisticsObjectService;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChangeRequestProcessorImpl implements ActionRequestProcessor {

    private final LogisticsObjectService logisticsObjectService;

    public ChangeRequestProcessorImpl(LogisticsObjectService logisticsObjectService) {
        this.logisticsObjectService = logisticsObjectService;
    }

    @Override
    public void process(LogisticsObject changeRequest) {
        LogisticsObject change = createChange(changeRequest.model());
        IRI logisticsObjectIRI = change.findIRI(API.hasLogisticsObject, true);
        LogisticsObject logisticsObject = getLogisticsObject(logisticsObjectIRI);
        if (logisticsObject == null) {
            log.warn("Change request {} do not contain logisticsObject reference.", changeRequest.iri());
            return;
        }
        logisticsObjectService.processChangeRequest(change, logisticsObject);
    }

    @Override
    public IRI requestType() {
        return API.ChangeRequest;
    }

    private static LogisticsObject createChange(Model model) {
        Model changeModel = RDFUtil.getSubModel(model, API.hasChange);
        IRI changeIRI = RDFUtil.getRootIri(changeModel);
        log.debug("Change IRI {} and Type {} ", changeIRI, API.Change);
        return new LogisticsObject(changeIRI, API.Change, changeModel);
    }

    private LogisticsObject getLogisticsObject(IRI logisticsObjectIRI) {
        return logisticsObjectService.findLogisticsObject(logisticsObjectIRI);
    }

}
