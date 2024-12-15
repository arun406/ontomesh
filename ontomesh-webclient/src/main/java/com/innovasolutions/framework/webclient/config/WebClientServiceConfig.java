package com.innovasolutions.framework.webclient.config;

import com.innovasolutions.framework.webclient.InMemoryRegisteredWebClientRepository;
import com.innovasolutions.framework.webclient.RegisteredWebClient;
import com.innovasolutions.framework.webclient.RegisteredWebClientConverter;
import com.innovasolutions.framework.webclient.RegisteredWebClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebClientServiceConfig {

    @Autowired
    private WebClientProperties webClientProperties;

    @Autowired
    private Logbook logbook;

    @Bean
    public RegisteredWebClientRepository webClientRepository(WebClientProperties properties) {
        List<RegisteredWebClient> registeredWebClients = new ArrayList<>();
        for (WebClientProperties.WebClientConfig webclient : properties.getWebclients()) {

            String clientId = webclient.getClientId();
            RegisteredWebClientConverter registeredWebClientConverter =
                    new RegisteredWebClientConverter(clientId, webclient.getBaseUrl(), logbook);
            for (WebClientProperties.WebClientConfig.ServiceConfig service : webclient.getServices()) {
                RegisteredWebClient registeredWebClient = registeredWebClientConverter.convert(service);
                registeredWebClients.add(registeredWebClient);
            }
        }
        return new InMemoryRegisteredWebClientRepository(registeredWebClients);
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate(RestTemplateBuilder builder, LogbookClientHttpRequestInterceptor interceptor) {
        return builder
                .additionalInterceptors(interceptor)
                .build();
    }
}
