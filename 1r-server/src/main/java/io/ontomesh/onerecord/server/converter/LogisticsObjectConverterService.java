package io.ontomesh.onerecord.server.converter;

import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class LogisticsObjectConverterService extends AbstractConverterService {

    @Override
    public OntoMeshId createUniqueUri() {
        return getIdProvider().createUniqueLoUri();
    }

    /**
     * Creates the LogisticsObject
     *
     * @param inputStream rdf stream.
     * @param rdfFormat   RDF Format
     * @return Logistics Object
     */
    public LogisticsObject getLogisticsObject(InputStream inputStream, RDFFormat rdfFormat) {

        Tuple Tuple = getTuple(inputStream, rdfFormat);
        if (Tuple == null) return null;

        return new LogisticsObject(Tuple.newIRI(), Tuple.model());
    }
}
