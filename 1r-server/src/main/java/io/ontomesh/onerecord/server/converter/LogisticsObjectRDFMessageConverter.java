package io.ontomesh.onerecord.server.converter;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.LogisticsObjectConverterService;
import io.ontomesh.onerecord.server.util.JsonldUtil;
import io.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
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
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class LogisticsObjectRDFMessageConverter extends AbstractHttpMessageConverter<LogisticsObject> {


    private final LogisticsObjectConverterService converterService;

    private final HttpServletRequest httpServletRequest;

    public LogisticsObjectRDFMessageConverter(LogisticsObjectConverterService converterService,
                                              HttpServletRequest httpServletRequest) {
        this.converterService = converterService;
        this.httpServletRequest = httpServletRequest;
    }

    protected boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(LogisticsObject.class);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(LogisticsObject.class)
                && (SemanticWebMediaType.APPLICATION_TURTLE.isCompatibleWith(mediaType)
                || SemanticWebMediaType.APPLICATION_JSON_LD.isCompatibleWith(mediaType)
                || SemanticWebMediaType.TEXT_TURTLE.isCompatibleWith(mediaType));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(LogisticsObject.class)
                && (SemanticWebMediaType.APPLICATION_TURTLE.isCompatibleWith(mediaType)
                || SemanticWebMediaType.APPLICATION_JSON_LD.isCompatibleWith(mediaType)
                || SemanticWebMediaType.TEXT_TURTLE.isCompatibleWith(mediaType));
    }

    protected LogisticsObject readInternal(Class<? extends LogisticsObject> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        InputStream inputStream = inputMessage.getBody();
        RDFFormat rdfFormat = RDFUtil.getFormat(Objects.requireNonNull(inputMessage.getHeaders().getContentType()));
        LogisticsObject logisticsObject = converterService.getLogisticsObject(inputStream, rdfFormat);
        if (logisticsObject == null) {
            throw new HttpMessageNotReadableException("Input is not a valid RDF.", inputMessage);
        }
        return logisticsObject;
    }

    @Override
    protected void writeInternal(LogisticsObject logisticsObject, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {
        log.debug("logistics object iri {}", logisticsObject.iri());
        MediaType mediaType = getAcceptMediaType();
        String rdfString = convertToString(logisticsObject, mediaType);
        if (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)) {
//            IRI type = RDFUtil.getType(logisticsObject.model(), logisticsObject.iri());
            IRI type = logisticsObject.type();
            rdfString = JsonldUtil.jsonLdToString(rdfString, true, type);
            Map<String, Map<String, String>> context = JsonldUtil.getContext();
//            context = new HashMap<>();
            Object compact = JsonLdProcessor.compact(JsonUtils.fromString(rdfString), context,
                    new JsonLdOptions());
            rdfString = JsonUtils.toPrettyString(compact);
        }
        StreamUtils.copy(rdfString.getBytes(Charset.defaultCharset()), outputMessage.getBody());
    }

    /**
     * Convert Logistics Object to String.
     *
     * @param logisticsObject Logistics Object
     * @param mediaType       Media Type
     * @return String.
     */
    private static String convertToString(LogisticsObject logisticsObject, MediaType mediaType) {
        ModelToStringConverter modelToStringConverter = getModelToStringConverter(mediaType);
        return modelToStringConverter.convert(logisticsObject.model());
    }

    /**
     * @return Media Type
     */
    private MediaType getAcceptMediaType() {
        String acceptHeader = httpServletRequest.getHeader(HttpHeaders.ACCEPT);
        MediaType mediaType = SemanticWebMediaType.APPLICATION_JSON_LD;
        // override the default with requested content type
        if (StringUtils.hasText(acceptHeader)) {
            mediaType = MediaType.valueOf(acceptHeader);
        }
        return mediaType;
    }

    private static ModelToStringConverter getModelToStringConverter(MediaType mediaType) {
        RDFFormat format = RDFUtil.getFormat(mediaType);
        return new ModelToStringConverter(format);
    }
}
