package com.ontomesh.onerecord.server.config;

import com.ontomesh.onerecord.server.converter.LogisticsObjectToRDFMessageConverter;
import com.ontomesh.onerecord.server.handler.ChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final ChangeHandler changeHandler;
    private final LogisticsObjectToRDFMessageConverter logisticsObjectToRdfMessageConverter;

    public WebConfig(ChangeHandler changeHandler, LogisticsObjectToRDFMessageConverter logisticsObjectToRdfMessageConverter) {
        this.changeHandler = changeHandler;
        this.logisticsObjectToRdfMessageConverter = logisticsObjectToRdfMessageConverter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(changeHandler).addPathPatterns("/logistics-objects/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        WebMvcConfigurer.super.configureMessageConverters(converters);
        converters.add(logisticsObjectToRdfMessageConverter);
    }
}
