package io.ontomesh.onerecord.server;

import io.ontomesh.onerecord.server.config.DatabaseConfig;
import org.eclipse.rdf4j.spring.RDF4JConfig;
import org.eclipse.rdf4j.spring.tx.TxConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan({"io.ontomesh.onerecord.server"})
@Import(value = {RDF4JConfig.class, TxConfig.class, DatabaseConfig.class})
public class OneRecordServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OneRecordServerApplication.class, args);
    }
}