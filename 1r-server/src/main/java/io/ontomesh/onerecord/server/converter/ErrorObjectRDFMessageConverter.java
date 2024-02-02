package io.ontomesh.onerecord.server.converter;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.ErrorObject;
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
import java.nio.charset.Charset;

@Slf4j
@Component
public class ErrorObjectRDFMessageConverter extends AbstractHttpMessageConverter<ErrorObject> {

    private final HttpServletRequest httpServletRequest;

    public ErrorObjectRDFMessageConverter(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    private static String getString(ErrorObject errorObject, MediaType selectedContentType) {
        ModelToStringConverter modelToStringConverter = getModelToStringConverter(selectedContentType);
        return modelToStringConverter.convert(errorObject.errorModel());
    }

    private static ModelToStringConverter getModelToStringConverter(MediaType selectedContentType) {
        RDFFormat format = RDFUtil.getFormat(selectedContentType);
        return new ModelToStringConverter(format);
    }

    private MediaType getAcceptMediaType() {
        String acceptHeader = httpServletRequest.getHeader(HttpHeaders.ACCEPT);
        MediaType selectedContentType = SemanticWebMediaType.APPLICATION_JSON_LD;
        // override the default with requested content type
        if (StringUtils.hasText(acceptHeader)) {
            selectedContentType = MediaType.valueOf(acceptHeader);
        }
        return selectedContentType;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ErrorObject.class);
    }

    @Override
    protected ErrorObject readInternal(Class<? extends ErrorObject> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(ErrorObject.class);
//                && mediaType != null && (mediaType.equals(SemanticWebMediaType.APPLICATION_TURTLE)
//                || mediaType.equals(SemanticWebMediaType.APPLICATION_JSON_LD)
//                || mediaType.equals(SemanticWebMediaType.TEXT_TURTLE));
    }

    @Override
    protected void writeInternal(ErrorObject errorObject, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        log.debug("error object {}", errorObject.iri());
        MediaType selectedContentType = getAcceptMediaType();
        String rdfString = getString(errorObject, selectedContentType);
        if (selectedContentType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)) {
            IRI type = RDFUtil.getType(errorObject.errorModel(), errorObject.iri());
            rdfString = JsonldUtil.jsonLdToString(rdfString, true, type);
            Object compact = JsonLdProcessor.compact(JsonUtils.fromString(rdfString), JsonldUtil.getContext(), new JsonLdOptions());
            rdfString = JsonUtils.toPrettyString(compact);
        }
        StreamUtils.copy(rdfString.getBytes(Charset.defaultCharset()), outputMessage.getBody());
    }
}
