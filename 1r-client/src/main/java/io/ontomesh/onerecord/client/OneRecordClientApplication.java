package io.ontomesh.onerecord.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan({"io.ontomesh.onerecord.client", "io.ontomesh.core"})
public class OneRecordClientApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(OneRecordClientApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("one record client stared......");
    }
}
