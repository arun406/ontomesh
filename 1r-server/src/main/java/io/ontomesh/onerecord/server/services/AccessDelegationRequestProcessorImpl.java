package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.actionrequests.ActionRequestProcessor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccessDelegationRequestProcessorImpl implements ActionRequestProcessor {
    @Override
    public void process(LogisticsObject actionRequest) {

    }

    @Override
    public IRI requestType() {
        return API.AccessDelegationRequest;
    }
}
