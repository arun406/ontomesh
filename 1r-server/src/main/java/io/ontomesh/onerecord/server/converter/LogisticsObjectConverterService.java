package io.ontomesh.onerecord.server.converter;

import io.ontomesh.core.properties.ServerProperties;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class LogisticsObjectConverterService extends AbstractConverterService {

    private final IdProvider idProvider;
    private final HttpServletRequest httpServletRequest;
    private final ServerProperties serverProperties;

    public LogisticsObjectConverterService(IdProvider idProvider, HttpServletRequest httpServletRequest, ServerProperties serverProperties) {
        this.idProvider = idProvider;
        this.httpServletRequest = httpServletRequest;
        this.serverProperties = serverProperties;
    }

    @Override
    public OntoMeshId createUniqueUri() {
        String url = getURL(this.httpServletRequest);
        if (url.contains("authorizations-acls")) {
            return this.idProvider.createAuthorizationLoUri();
        }
        return this.idProvider.createUniqueLoUri();
    }

    /**
     * Creates the LogisticsObject
     *
     * @param inputStream rdf stream.
     * @param rdfFormat   RDF Format
     * @return Logistics Object
     */
    public LogisticsObject getLogisticsObject(InputStream inputStream, RDFFormat rdfFormat) {

        Tuple Tuple = getTuple(inputStream, rdfFormat, this.serverProperties.getInternalIriScheme());
        if (Tuple == null) return null;

        return new LogisticsObject(Tuple.newIRI(), Tuple.model());
    }
}
