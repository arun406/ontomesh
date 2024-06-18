package io.ontomesh.onerecord.server.listener;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@Order(1)
@Slf4j
public class OneRecordDataModelInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final RepositoryConnection connection;

    public OneRecordDataModelInitializer(RepositoryConnection connection) {
        this.connection = connection;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.debug("Initializing the One Record Data Model ...");
        if (this.connection != null) {
            String ONE_RECORD_DATA_MODEL_FILENAME = "IATA-1R-DM-Ontology.ttl";
            try (InputStream input = this.getClass().getResourceAsStream("/" + ONE_RECORD_DATA_MODEL_FILENAME)) {
                // add the RDF data from the input stream directly to our database
                connection.add(input, "", RDFFormat.TURTLE);
            } catch (IOException e) {
                log.error("unable to initialize the one record data model ", e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean supportsAsyncExecution() {
        return ApplicationListener.super.supportsAsyncExecution();
    }
}
