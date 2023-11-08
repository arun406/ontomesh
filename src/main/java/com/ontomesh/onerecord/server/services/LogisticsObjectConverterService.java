package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.handler.BlankNodeReplacingStatementHandler;
import com.ontomesh.onerecord.server.util.RDFUtil;
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

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class LogisticsObjectConverterService {

    private final IRI hasPredefinedIri = SimpleValueFactory.getInstance().createIRI("https://www.ontomesh.com/onerecord#", "hasId");

    private final IdProvider idProvider;
    private final BlankNodeReplacingStatementHandler statementHandler;

    public LogisticsObjectConverterService(IdProvider idProvider, BlankNodeReplacingStatementHandler statementHandler) {
        this.idProvider = idProvider;
        this.statementHandler = statementHandler;
    }

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
        IRI newLoIri = loIri;
        if (isInternal) {
            newLoIri = idProvider.createUniqueLoUri().iri();
            log.info("new LO iri: {}", newLoIri);
            model = replaceRootIri(model, loIri, newLoIri);
        } else {
            // Adding marker
            model.add(loIri, hasPredefinedIri, Values.literal(true));
        }

        IRI type = RDFUtil.getType(model, newLoIri);
        LogisticsObject logisticsObject = new LogisticsObject(newLoIri, type, model);
        log.info("LO iri: {} ", logisticsObject.iri());
        return logisticsObject;
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
