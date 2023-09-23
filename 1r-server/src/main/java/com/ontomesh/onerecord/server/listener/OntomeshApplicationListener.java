package com.ontomesh.onerecord.server.listener;

import com.ontomesh.onerecord.server.services.ServerInformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(0)
@Slf4j
public class OntomeshApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ServerInformationService serverInformationService;

    public OntomeshApplicationListener(ServerInformationService serverInformationService) {
        this.serverInformationService = serverInformationService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.debug("OntomeshApplicationListener#onApplicationEvent");
        try {
            serverInformationService.createServerInformation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
