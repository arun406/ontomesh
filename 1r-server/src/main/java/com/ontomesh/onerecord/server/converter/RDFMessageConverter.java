package com.ontomesh.onerecord.server.converter;

import com.ontomesh.onerecord.server.api.BlankNodeReplacingStatementHandler;
import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.api.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

//@Component
@Slf4j
public class RDFMessageConverter extends AbstractHttpMessageConverter<LogisticsObject> {

    /**
     * The default charset used by the converter.
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


    @Autowired
    private IdProvider idProvider;

    public RDFMessageConverter() {
        super(SemanticWebMediaType.APPLICATION_JSON_LD);
    }

    protected boolean supports(Class<?> clazz) {
        return LogisticsObject.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    protected MediaType getDefaultContentType(LogisticsObject logisticsObject) {
        return SemanticWebMediaType.APPLICATION_JSON_LD;
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


    /**
     * @param rdfFormat
     * @param inputStream
     * @return
     * @throws IOException
     */
    private LogisticsObject getLogisticObject(RDFFormat rdfFormat, InputStream inputStream) throws IOException {
        log.debug("RDF Format: {}", rdfFormat.getName());
        RDFParser rdfParser = Rio.createParser(rdfFormat);

        BlankNodeReplacingStatementHandler statementHandler = new BlankNodeReplacingStatementHandler(idProvider);
        rdfParser.setRDFHandler(statementHandler);

        rdfParser.parse(inputStream);
        Model model = statementHandler.getModel();
        IRI loIri = getRootIri(model);
        log.info("root iri: {}", loIri);
        boolean isInternal = OntoMeshId.isInternalIri(loIri);
        IRI newLoIri = null;
        if (isInternal) {
            newLoIri = idProvider.createUniqueLoUri().getIri();
            log.info("new Lo Iri: {}", newLoIri);
            model = replaceRootIri(model, loIri, newLoIri);
        } else {
//            model.add(loIri, OntoMeshId.hasPredefinedIri, Values.literal(true));
        }
        LogisticsObject logisticsObject = new LogisticsObject(newLoIri, model);
        log.info("lo iri: {} ", logisticsObject.iri());
        return logisticsObject;
    }


    protected LogisticsObject readInternal(Class<? extends LogisticsObject> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
//        InputStream inputStream = inputMessage.getBody();
//        MediaType contentType = inputMessage.getHeaders().getContentType();
//        if (contentType == null) {
//            contentType = SemanticWebMediaType.APPLICATION_JSON_LD;
//        }
//        Charset charset = contentType.getCharset();
//        if (charset == null) {
//            charset = DEFAULT_CHARSET;
//        }
//        RDFFormat rdfFormat = getRdfFormat();
//        log.debug("RDF Format: {}", rdfFormat.getName());
//        RDFParser rdfParser = Rio.createParser(rdfFormat);
//        BlankNodeReplacingStatementHandler statementHandler = new BlankNodeReplacingStatementHandler(idProvider);
//        rdfParser.setRDFHandler(statementHandler);
//        rdfParser.parse(inputStream);
//
//        return getLogisticObject(getRdfFormat(), inputStream);
////        return statementHandler.getModel();
        return null;
    }

    private static RDFFormat getRdfFormat() {
        return RDFFormat.JSONLD;
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

    protected void writeInternal(LogisticsObject logisticsObject, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
//        if (logisticsObject != null) {
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            Rio.write(logisticsObject.model(), out, RDFFormat.JSONLD);
//            StreamUtils.copy(out.toByteArray(), outputMessage.getBody());
//        }

        throw new UnsupportedOperationException("This converter can only read objects, not write");
    }

}
