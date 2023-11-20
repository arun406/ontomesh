package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ActionRequestProcessorImpl implements ActionRequestProcessor {
    @Override
    public void process(LogisticsObject actionRequest) {
        log.debug("processing action request");
    }
}
