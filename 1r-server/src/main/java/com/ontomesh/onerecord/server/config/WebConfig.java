package com.ontomesh.onerecord.server.config;

import com.ontomesh.onerecord.server.api.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

//@EnableWebMvc
//@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private HttpMessageConverter<LogisticsObject> converter;

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .ignoreAcceptHeader(false)
                .mediaType(SemanticWebMediaType.APPLICATION_JSON_LD.getSubtype(), SemanticWebMediaType.APPLICATION_JSON_LD)
                .mediaType("json", MediaType.APPLICATION_JSON);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.debug("configureMessageConverters.size(): {}", converters.size());
        converters.add(converter);
    }
}
