package io.ontomesh.onerecord.client.handlers;

import io.ontomesh.onerecord.client.exception.OneRecordClientException;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

public class OneRecordClientExceptionHandler {

    public Mono<? extends Throwable> handleError(ClientResponse response) {
        if (response.statusCode().isError()) {
            return Mono.error(new OneRecordClientException(response.statusCode().toString()));
        }
        return Mono.empty();
    }
}
