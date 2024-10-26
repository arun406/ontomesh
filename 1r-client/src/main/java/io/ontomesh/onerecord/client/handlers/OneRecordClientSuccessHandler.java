package io.ontomesh.onerecord.client.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Slf4j
public class OneRecordClientSuccessHandler {

    public Mono<? extends Throwable> handleSuccess(ClientResponse response) {
        log.info("status code : {}", response);
        return Mono.empty();
    }
}
