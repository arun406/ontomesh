package io.ontomesh.onerecord.server.converter;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.handler.BlankNodeReplacingStatementHandler;
import io.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;

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
