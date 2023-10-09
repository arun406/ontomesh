package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;

@Slf4j
@Service
public class ServerInformationService {
    private final LogisticsObjectRepository repository;
    private final IdProvider idProvider;
    private final LogisticsObjectService logisticsObjectService;

    @Value("classpath:serverinfo/serverinfo.json")
    Resource serverInfo;

    public ServerInformationService(LogisticsObjectRepository repository, IdProvider idProvider, LogisticsObjectService logisticsObjectService) {
        this.repository = repository;
        this.idProvider = idProvider;
        this.logisticsObjectService = logisticsObjectService;
    }

    @Transactional
    public void createServerInformation() throws IOException {

        log.debug("creating the server information from the resource...");
        IRI dataOwnerIri = idProvider.getUriForLoId("_data-owner").iri();
        log.debug("data owner iri: {}", dataOwnerIri);
        InputStream inputStream = serverInfo.getInputStream();
        Model serverInfoModel = Rio.parse(inputStream, RDFFormat.JSONLD);
        repository.persist(dataOwnerIri, serverInfoModel);
    }

    @Transactional
    public LogisticsObject getServerInformation() {
        log.debug("Getting Server Information...");
        return logisticsObjectService.findLogisticsObject(URI.create("https://onerecord.ontomesh.com/"));
    }

    @Transactional
    public Set<String> getSupportedLogisticTypes() {
        LogisticsObject logisticsObject = getServerInformation();
        return Models.objectStrings(logisticsObject.model()
                .filter(logisticsObject.iri(), API.hasSupportedLogisticsObjectType, null));
    }
}
