package com.ontomesh.onerecord.server.config;

import com.ontomesh.onerecord.server.converter.LogigisticsObjectToRDFMessageConverter;
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
    private final LogigisticsObjectToRDFMessageConverter logigisticsObjectToRdfMessageConverter;

    public WebConfig(ChangeHandler changeHandler, LogigisticsObjectToRDFMessageConverter logigisticsObjectToRdfMessageConverter) {
        this.changeHandler = changeHandler;
        this.logigisticsObjectToRdfMessageConverter = logigisticsObjectToRdfMessageConverter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(changeHandler).addPathPatterns("/logistics-objects/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        WebMvcConfigurer.super.configureMessageConverters(converters);
        converters.add(logigisticsObjectToRdfMessageConverter);
    }
}
