package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.converter.InputStreamToModelConverter;
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
@RequiredArgsConstructor
public class LogisticsObjectConverterService {

    private final IdProvider idProvider;
    private final BlankNodeReplacingStatementHandler statementHandler;
    private final HttpServletRequest httpServletRequest;

    private OntoMeshId createUniqueUri() {
        String url = getURL();
        log.debug("URL : {} ", url);
        boolean isLogisticsEvent = url.contains("/logistics-events");
        if (isLogisticsEvent) {
            return idProvider.createUniqueLogisticsEventUri(URI.create(url));
        } else {
            return idProvider.createUniqueLoUri();
        }
    }

//    private IRI createUri() {
//        return idProvider.createUniqueUri().iri();
//        idProvider.createUniqueLogisticsEventUri(URI.create(url));
//    }

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
     * Creates the LogisticsObject
     *
     * @param inputStream rdf stream.
     * @param rdfFormat   RDF Format
     * @return Logistics Object
     */
    public LogisticsObject getLogisticsObject(InputStream inputStream, RDFFormat rdfFormat) {

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

        LogisticsObject logisticsObject = new LogisticsObject(newIRI, model);
        log.info("LO iri: {} ", logisticsObject.iri());
        return logisticsObject;
    }


//    /**
//     * @param inputStream input stream
//     * @return
//     * @throws IOException
//     */
//    public Model getModel(InputStream inputStream, RDFFormat rdfFormat) throws IOException {
//        RDFParser rdfParser = getParser(rdfFormat);
//        rdfParser.parse(inputStream);
//        return statementHandler.getModel();
//    }
//
//    private RDFParser getParser(RDFFormat rdfFormat) {
//        return RDFUtil.getParser(rdfFormat, statementHandler);
//    }
}
