package io.ontomesh.onerecord.server.converter.http;

import io.ontomesh.onerecord.server.converter.LogisticsObjectConverterService;
import io.ontomesh.onerecord.server.domain.ILogisticsObject;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class LogisticsObjectRDFMessageConverter extends AbstractHttpMessageConverter<LogisticsObject> {

    private final LogisticsObjectConverterService converterService;

    public LogisticsObjectRDFMessageConverter(LogisticsObjectConverterService converterService) {
        this.converterService = converterService;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return LogisticsObject.class.isAssignableFrom(clazz);
    }

    @Override
    protected ILogisticsObject convert(InputStream inputStream, RDFFormat rdfFormat) {
        return converterService.getLogisticsObject(inputStream, rdfFormat);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return LogisticsObject.class.isAssignableFrom(clazz) && isValid(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return LogisticsObject.class.isAssignableFrom(clazz) && isValid(mediaType);
    }
}
