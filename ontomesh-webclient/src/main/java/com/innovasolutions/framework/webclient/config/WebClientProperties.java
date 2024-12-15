package com.innovasolutions.framework.webclient.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
public class WebClientProperties {

    private List<WebClientConfig> webclients;

    public List<WebClientConfig> getWebclients() {
        return webclients;
    }

    public void setWebclients(List<WebClientConfig> webclients) {
        this.webclients = webclients;
    }

    public static class WebClientConfig {
        private String name;
        @NotBlank(message = "{clientId.not.blank}")
        private String clientId;

        @NotBlank(message = "{base-url.not.blank}")
        private String baseUrl;
        private String description;
        private List<ServiceConfig> services;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<ServiceConfig> getServices() {
            return services;
        }

        public void setServices(List<ServiceConfig> services) {
            this.services = services;
        }

        public static class ServiceConfig {
            @NotBlank(message = "service.key.not.null")
            private String key;

            private EndpointConfig endpoint;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public EndpointConfig getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(EndpointConfig endpoint) {
                this.endpoint = endpoint;
            }
        }

        public static class EndpointConfig {

            private String method;

            @NotBlank(message = "{endpoint.path.not.null}")
            private String path;
            private List<Header> headers;
            private String requestBody;
            private String responseBody;

            public String getMethod() {
                return method;
            }

            public void setMethod(String method) {
                this.method = method;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public List<Header> getHeaders() {
                return headers;
            }

            public void setHeaders(List<Header> headers) {
                this.headers = headers;
            }

            public String getRequestBody() {
                return requestBody;
            }

            public void setRequestBody(String requestBody) {
                this.requestBody = requestBody;
            }

            public String getResponseBody() {
                return responseBody;
            }

            public void setResponseBody(String responseBody) {
                this.responseBody = responseBody;
            }
        }

        public static class Header {
            private String key;
            private String value;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }
}