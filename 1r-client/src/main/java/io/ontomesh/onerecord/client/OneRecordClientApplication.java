package io.ontomesh.onerecord.client;

import io.ontomesh.onerecord.client.provider.IdProvider;
import io.ontomesh.onerecord.client.service.NotificationClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class OneRecordClientApplication implements CommandLineRunner {

    @Autowired
    private IdProvider idProvider;
    @Autowired
    private NotificationClientService notificationClientService;


    public static void main(String[] args) {
        log.info("STARTING THE APPLICATION");
        SpringApplication.run(OneRecordClientApplication.class, args);
        log.info("APPLICATION FINISHED");
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("EXECUTING : command line runner");
    }
}
