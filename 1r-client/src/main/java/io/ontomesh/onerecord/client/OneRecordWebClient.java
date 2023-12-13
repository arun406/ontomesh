package io.ontomesh.onerecord.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.ontomesh.onerecord.client.api.Constants;
import io.ontomesh.onerecord.client.handlers.OneRecordClientExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static io.ontomesh.onerecord.client.OneRecordClientLogger.logRequest;
import static io.ontomesh.onerecord.client.OneRecordClientLogger.logResponse;


@Slf4j
@Component
public class OneRecordWebClient {

    private final WebClient webClient;

    /**
     * constructor
     *
     * @param baseUrl base url
     */
    public OneRecordWebClient(@Value("${one-record.client.base-url:http://localhost:8090}") String baseUrl) {
        this.webClient = createDefaultClient(baseUrl);
    }

    private WebClient createDefaultClient(String baseUrl) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .wiretap(HttpClient.class.getCanonicalName(),
                        LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS));
                });

        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(clientCodecConfigurer -> {
                    clientCodecConfigurer.defaultCodecs().maxInMemorySize(1024 * 1024);
                })
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(httpHeaders -> {
                    MediaType mediaType = new MediaType("application", "ld+json");
                    httpHeaders.setContentType(mediaType);
                    httpHeaders.setContentLanguage(Locale.US);
                    httpHeaders.setAccept(List.of(mediaType));
                })
                .filters(exchangeFilterFunctions -> {
                    exchangeFilterFunctions.add(logRequest());
                    exchangeFilterFunctions.add(logResponse());
                })
                .build();
    }

    public void sendNotification(String notification) {
        OneRecordClientExceptionHandler handler = new OneRecordClientExceptionHandler();

        this.webClient
                .post()
                .uri(Constants.NOTIFICATION_URI)
                .body(BodyInserters.fromValue(notification))
                .retrieve()
                .onStatus(HttpStatusCode::isError, handler::handleError)
                .toBodilessEntity();

        // Block until the response is received and return the ResponseEntity.
//        ResponseEntity<Void> responseEntity = responseEntityMono.block();

    }

    public String initiateSubscription(IRI topic, IRI topicType) {

        OneRecordClientExceptionHandler exceptionHandler = new OneRecordClientExceptionHandler();

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.SUBSCRIPTION_URI)
                        .queryParam(Constants.QUERY_PARAM_TOPIC, topic)
                        .queryParam(Constants.QUERY_PARAM_TOPIC_TYPE, topicType)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, exceptionHandler::handleError)
                .bodyToMono(String.class)
                .block();
    }
}
