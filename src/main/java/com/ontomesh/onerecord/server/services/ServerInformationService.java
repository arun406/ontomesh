package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.MissingDataHolderException;
import com.ontomesh.onerecord.server.exception.ServerInfoNotFoundException;
import com.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.iata.onerecord.api.API;
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
//        IRI dataOwnerIri = idProvider.getUriForLoId("_data-owner").iri();
//        log.debug("data owner iri: {}", dataOwnerIri);
        InputStream inputStream = serverInfo.getInputStream();
        Model serverInfoModel = Rio.parse(inputStream, RDFFormat.JSONLD);
        repository.persist(null, serverInfoModel); //TODO revisit
    }

    public LogisticsObject getServerInformation() {
        log.debug("Getting Server Information...");
        return this.repository.findLoById(Values.iri("https://onerecord.ontomesh.com/"))
                .orElseThrow(() -> new ServerInfoNotFoundException("Server Information not defined."));
    }

    //    /**
//     * returns the supported logistics type IRIs.
//     *
//     * @return
//     */
//    public Set<IRI> getSupportedLogisticTypes() {
//        LogisticsObject logisticsObject = getServerInformation();
//        return Models.objectStrings(logisticsObject.model()
//                        .filter(logisticsObject.iri(), API.hasSupportedLogisticsObjectType, null))
//                .stream().map(Values::iri).collect(Collectors.toSet());
//    }
    public IRI getTenant() {
        LogisticsObject serverInformation = getServerInformation();
        return Models.objectIRI(serverInformation.model().filter(serverInformation.iri(), API.hasDataHolder, null))
                .orElseThrow(() -> new MissingDataHolderException("Data holder is not defined for the server information."));
    }
}
