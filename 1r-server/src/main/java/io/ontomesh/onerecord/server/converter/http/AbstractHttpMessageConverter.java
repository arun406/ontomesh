package io.ontomesh.onerecord.server.converter.http;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.domain.ILogisticsObject;
import io.ontomesh.onerecord.server.util.JsonldUtil;
import io.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractHttpMessageConverter<E extends ILogisticsObject>
        extends org.springframework.http.converter.AbstractHttpMessageConverter<E> {

    @Autowired
    private HttpServletRequest httpServletRequest;

    /**
     * Convert Logistics Object to String.
     *
     * @param mediaType Media Type
     * @param model
     * @return String.
     */
    protected static String convertToString(MediaType mediaType, Model model) {
        ModelToStringConverter modelToStringConverter = getModelToStringConverter(mediaType);
        return modelToStringConverter.convert(model);
    }

    /**
     * @return Media Type
     */
    protected MediaType getAcceptMediaType() {
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

    protected static boolean isValid(MediaType mediaType) {
        return SemanticWebMediaType.APPLICATION_TURTLE.isCompatibleWith(mediaType)
                || SemanticWebMediaType.APPLICATION_JSON_LD.isCompatibleWith(mediaType)
                || SemanticWebMediaType.TEXT_TURTLE.isCompatibleWith(mediaType);
    }


    @Override
    protected E readInternal(Class<? extends E> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        InputStream inputStream = inputMessage.getBody();
        RDFFormat rdfFormat = RDFUtil.getFormat(Objects.requireNonNull(inputMessage.getHeaders().getContentType()));
        ILogisticsObject l = convert(inputStream, rdfFormat);
        if (l == null) {
            throw new HttpMessageNotReadableException("Input is not a valid RDF.", inputMessage);
        }
        return (E) l;
    }

    protected abstract ILogisticsObject convert(InputStream inputStream, RDFFormat rdfFormat);

    @Override
    protected void writeInternal(E e, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        String rdfString = convertToRDFString(e);
        StreamUtils.copy(rdfString.getBytes(Charset.defaultCharset()), outputMessage.getBody());
    }

    protected abstract String convertToRDFString(E e) throws IOException;
}
