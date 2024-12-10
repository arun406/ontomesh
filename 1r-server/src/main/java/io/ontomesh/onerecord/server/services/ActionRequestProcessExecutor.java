package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author arun
 */
@Slf4j
@Service
public class ActionRequestProcessExecutor {

    private final Map<IRI, ActionRequestProcessor> actionRequestProcessors = new HashMap<>();

    /**
     * register the action processors. Called by individual processor implementation using @Autowired.
     *
     * @param iri
     * @param actionRequestProcessor
     */
    public void registerProcessor(IRI iri, ActionRequestProcessor actionRequestProcessor) {
        log.debug("registering the processors for {}", iri);
        this.actionRequestProcessors.put(iri, actionRequestProcessor);
    }

    public void execute(LogisticsObject actionRequest) {
        IRI requestType = actionRequest.type();
        ActionRequestProcessor actionRequestProcessor = actionRequestProcessors.get(requestType);
        if (actionRequestProcessor == null) {
            throw new RuntimeException(String.format("Action Request Processor is not defined for %s request type.", actionRequest.type()));
        }
        // call the process method.
        actionRequestProcessor.process(actionRequest);
    }
}
