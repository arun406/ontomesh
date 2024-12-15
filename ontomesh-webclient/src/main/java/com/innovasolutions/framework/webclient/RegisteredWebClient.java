package com.innovasolutions.framework.webclient;

import com.innovasolutions.framework.webclient.config.WebClientProperties;
import com.innovasolutions.framework.webclient.util.Utils;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.netty.LogbookClientHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.ClassUtils.forName;

@Slf4j
public class RegisteredWebClient {

    @Getter
    private final String clientId;
    private final WebClientProperties.WebClientConfig.ServiceConfig service;

    @Getter
    private final WebClient webClient;

    public RegisteredWebClient(String clientId, WebClientProperties.WebClientConfig.ServiceConfig service, String baseUri, Logbook logbook) {

        HttpClient httpClient = HttpClient.create()
                .wiretap(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new LogbookClientHandler(logbook)));

        this.clientId = clientId;
        this.service = service;
        Instant startTime = Instant.now();
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUri)
                .filter((request, next) -> next.exchange(request)
                        .doOnTerminate(() -> Utils.logExecutionTime(startTime))
                        .doOnError(ex -> log.error("Error occurred while executing the service : {} ", ex.getMessage(), ex))
                )
                .build();
    }

    public <S> Mono<S> execute() throws ClassNotFoundException {
        return execute(null, null, null, null);
    }

    public <R, S> Mono<S> execute(R body, HttpHeaders headers) throws ClassNotFoundException {
        return execute(body, null, null, headers);
    }

    public <R, S> Mono<S> execute(R body) throws ClassNotFoundException {
        return execute(body, null, null, null);
    }

    public <R, S> Mono<S> execute(R body, Map<String, String> pathVariables) throws ClassNotFoundException {
        return execute(body, pathVariables, null, null);
    }

    public <S> Mono<S> execute(Map<String, String> pathVariables) throws ClassNotFoundException {
        return execute(null, pathVariables, null, null);
    }

    /**
     * Execute the service with the provided request body, path variables, query parameters and headers
     *
     * @param body          Request body
     * @param pathVariables Path variables
     * @param queryParams   Query parameters
     * @param headers       Headers
     * @param <R>           Request body type
     * @param <S>           Response body type
     * @return Mono of the response body
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public <R, S> Mono<S> execute(R body, Map<String, String> pathVariables, Map<String, List<String>> queryParams, HttpHeaders headers) throws ClassNotFoundException {

        Class<S> elementClass = (Class<S>) forName(service.getEndpoint().getResponseBody(), null);

        WebClient.RequestBodySpec requestBodySpec = getWebClient().method(HttpMethod.valueOf(this.service.getEndpoint().getMethod()))
                .uri(uriBuilder -> {
                    uriBuilder.path(this.service.getEndpoint().getPath());
                    // Add query parameters if provided
                    if (queryParams != null) {
                        MultiValueMap<String, String> multiValueMap = CollectionUtils.toMultiValueMap(queryParams);
                        uriBuilder.queryParams(multiValueMap);
                    }
                    return pathVariables != null ? uriBuilder.build(pathVariables) : uriBuilder.build();
                });

        // Add headers if provided
        if (this.service.getEndpoint().getHeaders() != null) {
            this.service.getEndpoint().getHeaders()
                    .forEach(header -> requestBodySpec.header(header.getKey(), header.getValue()));
        }

        // add headers
        if (headers != null) {
            headers.forEach((key, value) -> requestBodySpec.header(key, String.valueOf(value)));
        }

        // Conditionally add the request body
        if (body != null) {
            requestBodySpec.bodyValue(body);
        }

        return requestBodySpec.retrieve()
                .bodyToMono(elementClass);
    }

    public String getServiceKey() {
        return this.service.getKey();
    }
}
