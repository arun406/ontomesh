package com.ontomesh.onerecord.server;

import com.ontomesh.onerecord.server.config.DatabaseConfig;
import org.eclipse.rdf4j.spring.RDF4JConfig;
import org.eclipse.rdf4j.spring.tx.TxConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan({"com.ontomesh.onerecord.server"})
@Import(value = {RDF4JConfig.class, TxConfig.class, DatabaseConfig.class})
public class OneRecordServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OneRecordServerApplication.class, args);
    }
}