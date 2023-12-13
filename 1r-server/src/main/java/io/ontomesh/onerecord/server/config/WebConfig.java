package io.ontomesh.onerecord.server.config;

import io.ontomesh.onerecord.server.converter.LogisticsObjectRDFMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final LogisticsObjectRDFMessageConverter logisticsObjectRdfMessageConverter;

    public WebConfig(LogisticsObjectRDFMessageConverter logisticsObjectRdfMessageConverter) {
        this.logisticsObjectRdfMessageConverter = logisticsObjectRdfMessageConverter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        WebMvcConfigurer.super.configureMessageConverters(converters);
        converters.add(logisticsObjectRdfMessageConverter);
    }
}
