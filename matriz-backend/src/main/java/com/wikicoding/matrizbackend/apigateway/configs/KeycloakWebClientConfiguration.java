package com.wikicoding.matrizbackend.apigateway.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KeycloakWebClientConfiguration {
    @Bean
    WebClient keycloakWebClient(@Value("${keycloak.url}") String keycloakUrl) {
        return WebClient.builder().baseUrl(keycloakUrl).build();
    }
}
