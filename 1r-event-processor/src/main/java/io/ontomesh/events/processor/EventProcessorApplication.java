package io.ontomesh.events.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class EventProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventProcessorApplication.class, args);
    }
}
