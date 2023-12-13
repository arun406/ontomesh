package io.ontomesh.onerecord.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

@Slf4j
public class OneRecordClientLogger {

    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request {} {}", clientRequest.method(), clientRequest.url());
                StringBuilder sb = new StringBuilder("Request: \n");
                clientRequest
                        .headers()
                        .forEach((name, values) -> values.forEach(value -> {
                                }
                                /* append header key/value */));
                log.debug(sb.toString());
            }
            return Mono.just(clientRequest);
        });
    }

    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("Response: \n");
                log.debug("Response Status {}", clientResponse.statusCode().value());
                clientResponse
                        .headers().asHttpHeaders().forEach((name, values) -> values.forEach(value -> {
                        }));

                log.debug(sb.toString());
            }
            return Mono.just(clientResponse);
        });
    }
}
