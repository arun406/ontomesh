package io.ontomesh.onerecord.server.converter;

import io.ontomesh.core.properties.ServerProperties;
import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class LogisticsEventConverterService extends AbstractConverterService {

    private final IdProvider idProvider;
    private final HttpServletRequest httpServletRequest;
    private final ServerProperties serverProperties;

    public LogisticsEventConverterService(IdProvider idProvider, HttpServletRequest httpServletRequest, ServerProperties serverProperties) {
        this.idProvider = idProvider;
        this.httpServletRequest = httpServletRequest;
        this.serverProperties = serverProperties;
    }

    @Override
    public OntoMeshId createUniqueUri() {
        String url = getURL(this.httpServletRequest);
        String logisticsObjectId = StringUtils.substringBetween(url, "logistics-objects/", "/logistics-events");
        return this.idProvider.createUniqueLogisticsEventUri(logisticsObjectId);
    }

    /**
     * Creates the LogisticsEvent
     *
     * @param inputStream rdf stream.
     * @param rdfFormat   RDF Format
     * @return Logistics Object
     */
    public LogisticsEvent getLogisticEvent(InputStream inputStream, RDFFormat rdfFormat) {

        Tuple Tuple = getTuple(inputStream, rdfFormat, this.serverProperties.getInternalIriScheme());
        if (Tuple == null) return null;

        return new LogisticsEvent(Tuple.newIRI(), Tuple.model());
    }
}
