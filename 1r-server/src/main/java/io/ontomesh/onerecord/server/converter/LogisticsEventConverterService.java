package io.ontomesh.onerecord.server.converter;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class LogisticsEventConverterService extends AbstractConverterService {

    @Override
    public OntoMeshId createUniqueUri() {
        String url = getURL();
        String logisticsObjectId = StringUtils.substringBetween(url, "logistics-objects/", "/logistics-events");
        return getIdProvider().createUniqueLogisticsEventUri(logisticsObjectId);
    }

    /**
     * Creates the LogisticsEvent
     *
     * @param inputStream rdf stream.
     * @param rdfFormat   RDF Format
     * @return Logistics Object
     */
    public LogisticsEvent getLogisticEvent(InputStream inputStream, RDFFormat rdfFormat) {

        Tuple Tuple = getTuple(inputStream, rdfFormat);
        if (Tuple == null) return null;

        return new LogisticsEvent(Tuple.newIRI(), Tuple.model());
    }
}
