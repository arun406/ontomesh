package io.ontomesh.onerecord.server.converter;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.handler.BlankNodeReplacingStatementHandler;
import io.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.URI;

@Getter
@Slf4j
public abstract class AbstractConverterService {

    @Autowired
    private IdProvider idProvider;
    @Autowired
    private BlankNodeReplacingStatementHandler statementHandler;
    @Autowired
    private HttpServletRequest httpServletRequest;

    protected String getURL() {
        return httpServletRequest.getRequestURL().toString();
    }

    public abstract OntoMeshId createUniqueUri();

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
     * @param rdfFormat
     * @return
     */
    protected Tuple getTuple(InputStream inputStream, RDFFormat rdfFormat) {
        Model model = new InputStreamToModelConverter(rdfFormat, statementHandler).convert(inputStream);
        if (model == null) {
            return null;
        }
        IRI loIRI = RDFUtil.getRootIri(model);
        log.info("root iri: {}", loIRI);

        boolean isInternal = OntoMeshId.isInternalIri(loIRI);
        log.info("is internal IRI? ,{}", isInternal);
        IRI newIRI = loIRI;
        if (isInternal) {
            newIRI = createUniqueUri().iri();
            log.info("new LO iri: {}", newIRI);
            model = replaceRootIri(model, loIRI, newIRI);
        } else {
            // Adding marker
            model.add(loIRI, OntoMesh.hasId, Values.literal(true));
        }
        return new Tuple(model, newIRI);
    }

    /**
     * Local Record to hold the data
     *
     * @param model
     * @param newIRI
     */
    protected record Tuple(Model model, IRI newIRI) {
    }
}
