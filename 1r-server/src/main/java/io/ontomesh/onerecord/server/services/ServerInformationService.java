package io.ontomesh.onerecord.server.services;

import io.ontomesh.core.properties.ServerProperties;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.MissingDataHolderException;
import io.ontomesh.onerecord.server.exception.ServerInfoNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerInformationService {

    @Autowired
    private ServerProperties serverProperties;

    private final LogisticsObjectRepository repository;

    @Value("classpath:serverinfo/serverinfo.json")
    Resource serverInfo;

    /**
     * creates the server information.
     *
     * @throws IOException exception
     */
    @Transactional
    public void createServerInformation() throws IOException {

        log.debug("creating the server information from the resource...");
        InputStream inputStream = serverInfo.getInputStream();
        Model serverInfoModel = Rio.parse(inputStream, RDFFormat.JSONLD);
        repository.persist(serverInfoModel); //TODO revisit
    }

    public LogisticsObject getServerInformation(String url) {
        log.info("Getting Server Information of {}", url);
        return this.repository.findLoById(Values.iri(url))
                .orElseThrow(() -> new ServerInfoNotFoundException("Server Information not defined."));
    }

    public IRI getTenant() {
        LogisticsObject serverInformation = getServerInformation(serverProperties.getUrl());
        return Models.objectIRI(serverInformation.model().filter(serverInformation.iri(), API.hasDataHolder, null))
                .orElseThrow(() -> new MissingDataHolderException("Data holder is not defined for the server information."));
    }

}
