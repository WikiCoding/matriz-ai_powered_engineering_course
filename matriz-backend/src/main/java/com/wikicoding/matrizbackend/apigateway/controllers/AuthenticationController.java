package com.wikicoding.matrizbackend.apigateway.controllers;

import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.time.Duration;
import java.util.Objects;

import com.wikicoding.matrizbackend.apigateway.dtos.KeycloakTokenResponse;
import com.wikicoding.matrizbackend.apigateway.dtos.LoginRequest;
import com.wikicoding.matrizbackend.apigateway.dtos.LogoutRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final WebClient keycloakWebClient;
    private final String realm;
    private final String clientId;
    private final ReactiveJwtDecoder jwtDecoder;

    public AuthenticationController(
            @Qualifier("keycloakWebClient") WebClient keycloakWebClient,
            @Value("${keycloak.realm:matriz}") String realm,
            @Value("${keycloak.client-id:matriz-client}") String clientId,
            ReactiveJwtDecoder jwtDecoder) {
        this.keycloakWebClient = keycloakWebClient;
        this.realm = realm;
        this.clientId = clientId;
        this.jwtDecoder = jwtDecoder;
    }

    @PostMapping(value = "login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<KeycloakTokenResponse> login(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required"));
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", request.username());
        form.add("password", request.password());

        return keycloakWebClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(WebClientResponseException.class, failure -> new ResponseStatusException(
                        failure.getStatusCode().is4xxClientError() ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY,
                        "Identity provider rejected login"))
                .flatMap(tokens -> jwtDecoder.decode(tokens.access_token())
                        .thenReturn(tokens));
    }

    @PostMapping(value = "logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(@AuthenticationPrincipal Jwt jwt, @RequestBody LogoutRequest request) {
        if (jwt == null) return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (request.refresh_token() == null || request.refresh_token().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token is required"));
        }
        // Only compare identity here. Keycloak must still validate the refresh token itself.
        try {
            var claims = JWTParser.parse(request.refresh_token()).getJWTClaimsSet();
            if (jwt.getClaimAsString("sid") == null
                    || !Objects.equals(jwt.getClaimAsString("sid"), claims.getStringClaim("sid"))
                    || !Objects.equals(jwt.getSubject(), claims.getSubject())
                    || jwt.getIssuer() == null
                    || !Objects.equals(jwt.getIssuer().toString(), claims.getIssuer())) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Refresh token must belong to the current session"));
            }
        } catch (ParseException exception) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid refresh token format"));
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("refresh_token", request.refresh_token());
        return keycloakWebClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve().toBodilessEntity().then()
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(failure -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Keycloak logout incomplete. Retry logout."));
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
