package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.handler.BlankNodeReplacingStatementHandler;
import com.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsObjectConverterService {

    private final IRI hasPredefinedIri = SimpleValueFactory.getInstance().createIRI("https://www.ontomesh.com/onerecord#", "hasId");

    private final IdProvider idProvider;
    private final BlankNodeReplacingStatementHandler statementHandler;
    private final HttpServletRequest httpServletRequest;

    /**
     * @param inputStream
     * @param rdfFormat
     * @return
     * @throws IOException
     */
    public LogisticsObject getLogisticObject(InputStream inputStream, RDFFormat rdfFormat) throws IOException {

        Model model = getModel(inputStream, rdfFormat);
        IRI loIri = RDFUtil.getRootIri(model);
        log.info("root iri: {}", loIri);

        boolean isInternal = OntoMeshId.isInternalIri(loIri);
        log.info("is internal IRI? ,{}", isInternal);
        IRI newLoIRI = loIri;
        if (isInternal) {
            URI path = URI.create(getURL());
//                    .getPath();
            boolean isLogisticsEvent = getURL().contains("/logistics-events");
            newLoIRI = getLogisticsObjectIRI(isLogisticsEvent, path);
            log.info("new LO iri: {}", newLoIRI);
            model = replaceRootIri(model, loIri, newLoIRI);
        } else {
            // Adding marker
            model.add(loIri, hasPredefinedIri, Values.literal(true));
        }

        IRI type = RDFUtil.getType(model, newLoIRI);
        LogisticsObject logisticsObject = new LogisticsObject(newLoIRI, type, model);
        log.info("LO iri: {} ", logisticsObject.iri());
        return logisticsObject;
    }

    private IRI getLogisticsObjectIRI(boolean isLogisticsEvent, URI path) {
        IRI newLoIri;
        if (isLogisticsEvent) {
            log.debug("is logistics-events request.");
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance().uri(path).path("/");
            newLoIri = idProvider.createUniqueLoUriForLogisticsEvent(builder).iri();
        } else {
            newLoIri = idProvider.createUniqueLoUri().iri();
        }
        return newLoIri;
    }

    private String getURL() {
        return httpServletRequest.getRequestURL().toString();
    }

    private Model replaceRootIri(Model model, IRI oldRootIri, IRI newLoIri) {
        return model.stream().map(statement -> {
            Statement updatedStatement;
            if (statement.getSubject().isIRI() && oldRootIri.equals(statement.getSubject())) {
                updatedStatement = Statements.statement(
                        newLoIri,
                        statement.getPredicate(),
                        statement.getObject(),
                        statement.getContext()
                );
            } else if (statement.getObject().isIRI() && oldRootIri.equals(statement.getObject())) {
                updatedStatement = Statements.statement(
                        statement.getSubject(),
                        statement.getPredicate(),
                        newLoIri,
                        statement.getContext()
                );
            } else {
                updatedStatement = statement;
            }
            return updatedStatement;
        }).collect(ModelCollector.toModel());
    }


    /**
     * @param inputStream
     * @return
     * @throws IOException
     */
    public Model getModel(InputStream inputStream, RDFFormat rdfFormat) throws IOException {
        RDFParser rdfParser = getParser(rdfFormat);
        rdfParser.parse(inputStream);
        return statementHandler.getModel();
    }

    private RDFParser getParser(RDFFormat rdfFormat) {
        return RDFUtil.getParser(rdfFormat, statementHandler);
    }
}
