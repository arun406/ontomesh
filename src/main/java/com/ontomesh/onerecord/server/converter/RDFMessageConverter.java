package com.ontomesh.onerecord.server.converter;

import com.ontomesh.onerecord.server.api.BlankNodeReplacingStatementHandler;
import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.api.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import com.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RDFMessageConverter extends AbstractHttpMessageConverter<LogisticsObject> {

    private final IdProvider idProvider;
    private final BlankNodeReplacingStatementHandler statementHandler;
    /**
     * The default charset used by the converter.
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private final IRI hasPredefinedIri = SimpleValueFactory.getInstance().createIRI("https://www.ontomesh.com/onerecord#", "hasId");


    public RDFMessageConverter(IdProvider idProvider, BlankNodeReplacingStatementHandler statementHandler) {
        this.idProvider = idProvider;
        this.statementHandler = statementHandler;
    }

    protected boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(LogisticsObject.class);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(LogisticsObject.class)
                && (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_TURTLE)
                || mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)
                || mediaType.isCompatibleWith(SemanticWebMediaType.TEXT_TURTLE));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(LogisticsObject.class)
                && (mediaType.equals(SemanticWebMediaType.APPLICATION_TURTLE)
                || mediaType.equals(SemanticWebMediaType.APPLICATION_JSON_LD)
                || mediaType.equals(SemanticWebMediaType.TEXT_TURTLE));
    }

    protected LogisticsObject readInternal(Class<? extends LogisticsObject> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        InputStream inputStream = inputMessage.getBody();

        RDFFormat rdfFormat = getRDFFormat(inputMessage.getHeaders().getContentType());
        return getLogisticObject(inputStream, rdfFormat);
    }

    protected void writeInternal(LogisticsObject logisticsObject, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
//        if (logisticsObject != null) {
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            Rio.write(logisticsObject.model(), out, RDFFormat.JSONLD);
//            StreamUtils.copy(out.toByteArray(), outputMessage.getBody());
//        }
        log.debug("logistics object iri :{}", logisticsObject.iri());
        log.debug("content Type: {}", outputMessage.getHeaders().getContentType());

    }


    /////////////////////////////////////////////*****************\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

    /**
     * @param inputStream
     * @param rdfFormat
     * @return
     * @throws IOException
     */
    protected LogisticsObject getLogisticObject(InputStream inputStream, RDFFormat rdfFormat) throws IOException {

        Model model = getModel(inputStream, rdfFormat);
        IRI loIri = getRootIri(model);
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

    private RDFFormat getRDFFormat(MediaType contentType) {
        if (contentType == null) {
            return RDFFormat.JSONLD;
        } else if (contentType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)) {
            return RDFFormat.JSONLD;
        } else if (contentType.isCompatibleWith(SemanticWebMediaType.APPLICATION_TURTLE)) {
            return RDFFormat.TURTLE;
        } else if (contentType.isCompatibleWith(SemanticWebMediaType.TEXT_TURTLE)) {
            return RDFFormat.TURTLE;
        }
        throw new RuntimeException("Unsupported media type [" + contentType + "]");
    }

    private RDFFormat getRdfFormat(String contentType) {
        String[] split = contentType.split(";");
        return switch (split[0]) {
            case SemanticWebMediaType.APPLICATION_JSON_LD_VALUE -> RDFFormat.JSONLD;
            case SemanticWebMediaType.TEXT_TURTLE_VALUE, SemanticWebMediaType.APPLICATION_TURTLE_VALUE ->
                    RDFFormat.TURTLE;
            default -> throw new RuntimeException("Unsupported media type [" + contentType + "]");
        };
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
