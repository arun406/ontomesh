package io.ontomesh.onerecord.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.ontomesh.onerecord.client.OneRecordClient;
import io.ontomesh.onerecord.client.exception.OneRecordClientException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static io.ontomesh.onerecord.client.http.OneRecordClientLogger.logRequest;
import static io.ontomesh.onerecord.client.http.OneRecordClientLogger.logResponse;

@Slf4j
@Configuration
public class WebConfig {


    @Bean
    public WebClient webClient(ObjectMapper objectMapper) {

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
//                .baseUrl(baseUrl)
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
                .defaultStatusHandler(HttpStatusCode::isError, clientResponse -> Mono.error(new OneRecordClientException(clientResponse.statusCode().toString())))
                .defaultStatusHandler(HttpStatusCode::is2xxSuccessful, clientResponse -> {
                    log.info("status code : {}", clientResponse.statusCode());
                    return Mono.empty();
                })
                .build();
    }

    @SneakyThrows
    @Bean
    public OneRecordClient oneRecordClient(WebClient webClient) {
        HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                        .build();
        return httpServiceProxyFactory.createClient(OneRecordClient.class);
    }
}
