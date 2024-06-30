package io.ontomesh.onerecord.server.converter.http;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.converter.LogisticsEventConverterService;
import io.ontomesh.onerecord.server.domain.ILogisticsObject;
import io.ontomesh.onerecord.server.domain.LogisticsEvent;
import io.ontomesh.onerecord.server.util.JsonldUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Component
public class LogisticsEventRDFMessageConverter extends AbstractHttpMessageConverter<LogisticsEvent> {


    private final LogisticsEventConverterService converterService;

    public LogisticsEventRDFMessageConverter(LogisticsEventConverterService converterService) {
        this.converterService = converterService;
    }

    protected boolean supports(Class<?> clazz) {
        return LogisticsEvent.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return LogisticsEvent.class.isAssignableFrom(clazz) && isValid(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return LogisticsEvent.class.isAssignableFrom(clazz) && isValid(mediaType);
    }

    @Override
    protected ILogisticsObject convert(InputStream inputStream, RDFFormat rdfFormat) {
        return converterService.getLogisticEvent(inputStream, rdfFormat);
    }

    @Override
    protected String convertToRDFString(LogisticsEvent event) throws IOException {
        MediaType mediaType = getAcceptMediaType();
        String rdfString = convertToString(mediaType, event.model());
        if (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)) {
            rdfString = JsonldUtil.jsonLdToString(rdfString, true, event.getType());
            Map<String, Map<String, String>> context = JsonldUtil.getContext();
            Object compact = JsonLdProcessor.compact(JsonUtils.fromString(rdfString), context, new JsonLdOptions());
            rdfString = JsonUtils.toPrettyString(compact);
        }
        return rdfString;
    }
}
