package com.ontomesh.onerecord.server.converter;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.api.OntoMeshId;
import com.ontomesh.onerecord.server.common.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.handler.BlankNodeReplacingStatementHandler;
import com.ontomesh.onerecord.server.util.JsonldUtil;
import com.ontomesh.onerecord.server.util.ModelToStringConverter;
import com.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class LogisticsObjectRDFMessageConverter extends AbstractHttpMessageConverter<LogisticsObject> {

    private final IdProvider idProvider;
    private final BlankNodeReplacingStatementHandler statementHandler;

    private final HttpServletRequest httpServletRequest;
    /**
     * The default charset used by the converter.
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private final IRI hasPredefinedIri = SimpleValueFactory.getInstance().createIRI("https://www.ontomesh.com/onerecord#", "hasId");


    public LogisticsObjectRDFMessageConverter(IdProvider idProvider, BlankNodeReplacingStatementHandler statementHandler, HttpServletRequest httpServletRequest) {
        this.idProvider = idProvider;
        this.statementHandler = statementHandler;
        this.httpServletRequest = httpServletRequest;
    }

    protected boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(LogisticsObject.class);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(LogisticsObject.class)
                && mediaType != null && (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_TURTLE)
                || mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)
                || mediaType.isCompatibleWith(SemanticWebMediaType.TEXT_TURTLE));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(LogisticsObject.class)
                && mediaType != null && (mediaType.equals(SemanticWebMediaType.APPLICATION_TURTLE)
                || mediaType.equals(SemanticWebMediaType.APPLICATION_JSON_LD)
                || mediaType.equals(SemanticWebMediaType.TEXT_TURTLE));
    }

    protected LogisticsObject readInternal(Class<? extends LogisticsObject> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        InputStream inputStream = inputMessage.getBody();
        RDFFormat rdfFormat = getRDFFormat(inputMessage.getHeaders().getContentType());
        return getLogisticObject(inputStream, rdfFormat);
    }

    @Override
    protected void writeInternal(LogisticsObject logisticsObject, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        log.debug("logistics object iri {}", logisticsObject.iri());
        MediaType selectedContentType = getAcceptMediaType();
        String rdfString = getString(logisticsObject, selectedContentType);
        if (selectedContentType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)) {
            IRI type = RDFUtil.getType(logisticsObject.model(), logisticsObject.iri());
            rdfString = JsonldUtil.jsonLdToString(rdfString, true, type);
            Object compact = JsonLdProcessor.compact(JsonUtils.fromString(rdfString), JsonldUtil.getContext(), new JsonLdOptions());
            rdfString = JsonUtils.toPrettyString(compact);
        }
        StreamUtils.copy(rdfString.getBytes(Charset.defaultCharset()), outputMessage.getBody());
    }

    /**
     * @param logisticsObject
     * @param selectedContentType
     * @return
     */
    private static String getString(LogisticsObject logisticsObject, MediaType selectedContentType) {
        ModelToStringConverter modelToStringConverter = getModelToStringConverter(selectedContentType);
        return modelToStringConverter.convert(logisticsObject.model());
    }

    /**
     * @return
     */
    private MediaType getAcceptMediaType() {
        String acceptHeader = httpServletRequest.getHeader(HttpHeaders.ACCEPT);
        MediaType selectedContentType = SemanticWebMediaType.APPLICATION_JSON_LD;
        // override the default with requested content type
        if (StringUtils.hasText(acceptHeader)) {
            selectedContentType = MediaType.valueOf(acceptHeader);
        }
        return selectedContentType;
    }

    private static ModelToStringConverter getModelToStringConverter(MediaType selectedContentType) {
        RDFFormat format = RDFUtil.getFormat(selectedContentType);
        return new ModelToStringConverter(format);
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
