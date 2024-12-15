package com.innovasolutions.framework.webclient;

import com.innovasolutions.framework.webclient.config.WebClientProperties;
import org.springframework.core.convert.converter.Converter;
import org.zalando.logbook.Logbook;

public final class RegisteredWebClientConverter implements Converter<WebClientProperties.WebClientConfig.ServiceConfig, RegisteredWebClient> {
    private final String baseUri;
    private final String clientId;
    private final Logbook logbook;

    public RegisteredWebClientConverter(String clientId, String baseUri, Logbook logbook) {
        this.baseUri = baseUri;
        this.clientId = clientId;
        this.logbook = logbook;
    }

    public RegisteredWebClient convert(WebClientProperties.WebClientConfig.ServiceConfig service) {
        return new RegisteredWebClient(this.clientId, service, this.baseUri, this.logbook);
    }
}
