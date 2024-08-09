package io.ontomesh.events.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan({"io.ontomesh.events.processor", "io.ontomesh.core"})
public class EventProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventProcessorApplication.class, args);
    }
}
