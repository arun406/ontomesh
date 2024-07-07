package io.ontomesh.onerecord.server.converter.http;

import io.ontomesh.onerecord.server.converter.LogisticsEventConverterService;
import io.ontomesh.onerecord.server.domain.ILogisticsObject;
import io.ontomesh.onerecord.server.domain.LogisticsEvents;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class LogisticsEventsRDFMessageConverter extends AbstractHttpMessageConverter<LogisticsEvents> {


    private final LogisticsEventConverterService converterService;

    public LogisticsEventsRDFMessageConverter(LogisticsEventConverterService converterService) {
        this.converterService = converterService;
    }

    protected boolean supports(Class<?> clazz) {
        return LogisticsEvents.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return LogisticsEvents.class.isAssignableFrom(clazz) && isValid(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return LogisticsEvents.class.isAssignableFrom(clazz) && isValid(mediaType);
    }

    @Override
    protected ILogisticsObject convert(InputStream inputStream, RDFFormat rdfFormat) {
        return converterService.getLogisticEvent(inputStream, rdfFormat);
    }
}
