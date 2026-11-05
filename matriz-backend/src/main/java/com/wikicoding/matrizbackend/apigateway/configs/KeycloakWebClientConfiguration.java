package com.wikicoding.matrizbackend.apigateway.configs;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class KeycloakWebClientConfiguration {
    @Bean
    WebClient keycloakWebClient(@Value("${keycloak.url}") String keycloakUrl) {
        return WebClient.builder().baseUrl(keycloakUrl)
                .filter((request, next) -> Mono.defer(() -> {
                    long started = System.nanoTime();
                    // Never log resolved URLs, headers or provider payloads: they contain identity data.
                    return next.exchange(request)
                            .doOnNext(response -> log.debug("Keycloak method={} status={} durationMs={}",
                                    request.method(), response.statusCode().value(),
                                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)))
                            .doOnError(failure -> log.debug("Keycloak transport failure method={} exception={} durationMs={}",
                                    request.method(), failure.getClass().getSimpleName(),
                                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
                })).build();
    }
}
