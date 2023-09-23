package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.AlreadyExistsException;
import com.ontomesh.onerecord.server.exception.InvalidAddressException;
import com.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import com.ontomesh.onerecord.server.services.LoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class BaseController {
    @Autowired
    protected IdProvider idProvider;
    @Autowired
    private LoService loService;
    private SimpleValueFactory instance = SimpleValueFactory.getInstance();

    private IRI hasPredefinedIri = instance.createIRI("https://www.ontomesh.com/onerecord#", "hasId");

    public void removeMarkerPredicate(LogisticsObject logisticsObject) {
        logisticsObject.model().remove(logisticsObject.iri(), hasPredefinedIri, null);
    }

    protected static String getLoType(LogisticsObject logisticsObject) {
        return Models.objectString(logisticsObject.model().filter(logisticsObject.iri(), RDF.TYPE, null))
                .orElseThrow();
    }

    protected OntoMeshId getLoId(String uri) {
        return idProvider.getUriForLoId(uri);
    }

    protected void checkForDuplicateIRI(LogisticsObject logisticsObject) {
        IRI iri = logisticsObject.iri();
        // Check for proper address path.
        if (!iri.toString().contains(idProvider.getLogisticsObjectBaseIRI().toString())) {
            throw new InvalidAddressException(iri, idProvider.getLogisticsObjectBaseIRI());
        }

        // Check if IRI is already taken.
        boolean exists = loService.isExists(iri);
        if (exists)
            throw new AlreadyExistsException(iri, getLoType(logisticsObject));
    }

    protected boolean isIdPresent(LogisticsObject logisticsObject) {
        // Remove predicate temporarily introduced by LogisticsObjectBodyReader. Information is still part
        // of metadata.

        Optional<Literal> hasPredefIri =
                Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), hasPredefinedIri, null));
        return hasPredefIri.map(Literal::booleanValue).orElse(false);
    }

    /**
     * @param contentType
     * @param input
     * @return
     * @throws IOException
     */
    protected LogisticsObject getLogisticObject(String contentType, String input) throws IOException {
        RDFFormat rdfFormat = getRdfFormat(contentType);
        log.debug("RDF Format: {}", rdfFormat.getName());
        RDFParser rdfParser = Rio.createParser(rdfFormat);

        BlankNodeReplacingStatementHandler statementHandler = new BlankNodeReplacingStatementHandler(idProvider);
        rdfParser.setRDFHandler(statementHandler);

        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        rdfParser.parse(inputStream);
        Model model = statementHandler.getModel();
        IRI loIri = getRootIri(model);
        log.info("root iri: {}", loIri);
        boolean isInternal = OntoMeshId.isInternalIri(loIri);
        log.info("is internal IRI? ,{}", isInternal);
        IRI newLoIri = loIri;
        if (isInternal) {
            newLoIri = idProvider.createUniqueLoUri().getIri();
            log.info("new Lo Iri: {}", newLoIri);
            model = replaceRootIri(model, loIri, newLoIri);
        } else {
            // Adding marker
            model.add(loIri, hasPredefinedIri, Values.literal(true));
        }
        LogisticsObject logisticsObject = new LogisticsObject(newLoIri, model);
        log.info("lo iri: {} ", logisticsObject.iri());
        return logisticsObject;
    }

    protected static HttpHeaders getResponseHeaders(URI loURI, String loType, MediaType contentType) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(CommonConstants.LO_TYPE_HEADER, CommonConstants.LO_TYPE_COMPANY_INFORMATION);
        responseHeaders.setContentType(contentType);
        responseHeaders.add("Type", loType);
        responseHeaders.setLocation(loURI);
        return responseHeaders;
    }


    public HttpHeaders getResponseHeaders(URI loUri, LogisticsObject logisticsObject, MediaType contentType) {
        String loType = getLoType(logisticsObject);
        return getResponseHeaders(loUri, loType, contentType);
    }

    protected static HttpHeaders getResponseHeaders(MediaType contentType) {

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(contentType);
        responseHeaders.setContentLanguage(Locale.ENGLISH);
        return responseHeaders;
    }

    protected static HttpHeaders getResponseHeaders(HttpServletRequest httpServletRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));
        headers.setLocation(URI.create(httpServletRequest.getRequestURL().toString()));
        headers.add("Revision-Header", UUID.randomUUID().toString());
        headers.add(CommonConstants.LO_TYPE_HEADER, CommonConstants.LO_TYPE_COMPANY_INFORMATION);
        return headers;
    }

    private static RDFFormat getRdfFormat(String contentType) {
        String[] split = contentType.split(";");
        return switch (split[0]) {
            case SemanticWebMediaType.APPLICATION_JSON_LD_VALUE -> RDFFormat.JSONLD;
            case SemanticWebMediaType.TEXT_TURTLE_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE ->
                    RDFFormat.TURTLE;
            default -> throw new RuntimeException("Unsupported media type [" + contentType + "]");
        };
    }

    public IRI getRootIri(Model model) {
        // set of all subject IRIs
        Set<IRI> subjectIris = Models.subjectIRIs(model);
        // set of all object IRIs
        Set<IRI> objectIris = Models.objectIRIs(model);

        // subtract set of objects from set of subjects
        subjectIris.removeAll(objectIris);

        // if more
        if (subjectIris.size() != 1) {
            throw new InvalidApiRequestException("Invalid number of IRIs found, expected 1 got: " + subjectIris.stream()
                    .map(IRI::stringValue)
                    .collect(Collectors.joining(",", "[", "]"))
            );
        }

        return subjectIris.iterator().next();
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


}
