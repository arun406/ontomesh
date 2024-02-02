package io.ontomesh.onerecord.server.listener;

import io.ontomesh.onerecord.server.services.ServerInformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(0)
@Slf4j
public class OneRecordInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final ServerInformationService serverInformationService;

    public OneRecordInitializer(ServerInformationService serverInformationService) {
        this.serverInformationService = serverInformationService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.debug("OnerecordInitializer#onApplicationEvent");
        try {
            serverInformationService.createServerInformation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
