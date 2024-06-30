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
