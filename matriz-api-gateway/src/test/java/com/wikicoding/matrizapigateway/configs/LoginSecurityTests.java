package com.wikicoding.matrizapigateway.configs;

import com.wikicoding.matrizapigateway.controllers.AuthenticationController;
import com.wikicoding.matrizapigateway.security.SessionRegistry;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {SecurityConfiguration.class, LoginSecurityTests.TestConfiguration.class})
class LoginSecurityTests {
    @Autowired ApplicationContext context;
    @Autowired TestState state;
    @Autowired SessionRegistry sessions;
    @Autowired ReactiveStringRedisTemplate redis;
    WebTestClient client;

    @BeforeEach
    void resetState() {
        state.values.clear();
        state.redisDown.set(false);
        state.providerDown.set(false);
        state.backendDown.set(false);
        state.logoutCalls.set(0);
        state.createdUsers.set(0);
        state.deletedUsers.set(0);
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void loginAcceptsAnonymousCredentialsAndRegistersSession() {
        client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"test\",\"password\":\"test\"}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.access_token").isEqualTo("valid");
        protectedRequest("valid").expectStatus().isOk();
    }

    @Test
    void administratorRegistrationCreatesIdentityRoleAndLocalProfile() {
        sessions.register(jwt("admin")).block();
        client.post().uri("/api/v1/auth/register").headers(h -> h.setBearerAuth("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"new.user\",\"email\":\"new.user@example.com\",\"password\":\"a-long-temporary-password\",\"role\":\"SOFTWARE_ENGINEER\",\"teamId\":\"4e39c0f2-90b0-44d5-bc8c-968d365d3375\"}")
                .exchange().expectStatus().isCreated().expectBody().jsonPath("$.email").isEqualTo("new.user@example.com");
        assertEquals(1, state.createdUsers.get());
        assertEquals(0, state.deletedUsers.get());
    }

    @Test
    void registrationRejectsNonAdministrators() {
        sessions.register(jwt("valid")).block();
        client.post().uri("/api/v1/auth/register").headers(h -> h.setBearerAuth("valid"))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange().expectStatus().isForbidden();
    }

    @Test
    void registrationDeletesKeycloakIdentityWhenLocalProfileCreationFails() {
        sessions.register(jwt("admin")).block();
        state.backendDown.set(true);
        client.post().uri("/api/v1/auth/register").headers(h -> h.setBearerAuth("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"new.user\",\"email\":\"new.user@example.com\",\"password\":\"a-long-temporary-password\",\"role\":\"SOFTWARE_ENGINEER\",\"teamId\":\"4e39c0f2-90b0-44d5-bc8c-968d365d3375\"}")
                .exchange().expectStatus().isEqualTo(502);
        assertEquals(1, state.deletedUsers.get());
    }

    @Test
    void protectedRoutesAndLogoutRequireAuthentication() {
        client.get().uri("/api/v1/me").exchange().expectStatus().isUnauthorized();
        logout(null, "refresh").expectStatus().isUnauthorized();
        logout("invalid", "refresh").expectStatus().isUnauthorized();
        assertEquals(0, state.logoutCalls.get());
    }

    @Test
    void logoutRejectsReplayAndOtherTokensFromSameSessionAcrossInstances() {
        sessions.register(jwt("valid")).block();
        var otherInstance = new SessionRegistry(redis);
        assertEquals(Boolean.TRUE, otherInstance.isActive(jwt("renewed")).block());
        logout("valid", "refresh").expectStatus().isNoContent();
        protectedRequest("valid").expectStatus().isUnauthorized();
        protectedRequest("renewed").expectStatus().isUnauthorized();
        assertNotEquals(Boolean.TRUE, otherInstance.isActive(jwt("renewed")).block());
        assertEquals(1, state.logoutCalls.get());
    }

    @Test
    void logoutDoesNotRevokeAnotherSession() {
        sessions.register(jwt("valid")).block();
        sessions.register(jwt("other")).block();
        logout("valid", "refresh").expectStatus().isNoContent();
        protectedRequest("other").expectStatus().isOk();
    }

    @Test
    void keycloakFailureKeepsSessionRevokedAndAllowsLogoutRetry() {
        sessions.register(jwt("valid")).block();
        state.providerDown.set(true);
        logout("valid", "refresh").expectStatus().isEqualTo(503);
        protectedRequest("valid").expectStatus().isUnauthorized();
        state.providerDown.set(false);
        logout("valid", "refresh").expectStatus().isNoContent();
        protectedRequest("valid").expectStatus().isUnauthorized();
    }

    @Test
    void redisFailureNeverReportsSuccessfulLogoutOrGrantsAccess() {
        sessions.register(jwt("valid")).block();
        state.redisDown.set(true);
        logout("valid", "refresh").expectStatus().isEqualTo(503);
        protectedRequest("valid").expectStatus().isEqualTo(503);
        assertEquals(0, state.logoutCalls.get());
    }

    @Test
    void missingStateAndDataLossRequireNewLogin() {
        protectedRequest("valid").expectStatus().isUnauthorized();
        sessions.register(jwt("valid")).block();
        state.values.clear();
        protectedRequest("valid").expectStatus().isUnauthorized();
    }

    @Test
    void loginRegistrationCannotUndoRevocation() {
        sessions.revoke(jwt("valid")).block();
        assertThrows(RuntimeException.class, () -> sessions.register(jwt("valid")).block());
        protectedRequest("valid").expectStatus().isUnauthorized();
    }

    @Test
    void logoutRequiresRefreshToken() {
        sessions.register(jwt("valid")).block();
        logout("valid", "").expectStatus().isBadRequest();
        protectedRequest("valid").expectStatus().isOk();
        assertEquals(0, state.logoutCalls.get());
    }

    @Test
    void logoutRejectsRefreshTokenFromAnotherSession() {
        sessions.register(jwt("valid")).block();
        logout("valid", "other").expectStatus().isBadRequest();
        protectedRequest("valid").expectStatus().isOk();
        assertEquals(0, state.logoutCalls.get());
    }

    @Test
    void sessionWithoutIdAndSessionPastAbsoluteMaximumAreRejected() {
        var missingSid = Jwt.withTokenValue("missing").header("alg", "RS256")
                .issuer("https://identity.example/realms/matriz")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        assertThrows(RuntimeException.class, () -> sessions.register(missingSid).block());
        var oldSession = Jwt.withTokenValue("old").header("alg", "RS256")
                .issuer("https://identity.example/realms/matriz").claim("sid", "old")
                .claim("auth_time", Instant.now().minusSeconds(9 * 3600))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        assertThrows(RuntimeException.class, () -> sessions.register(oldSession).block());
    }

    private WebTestClient.ResponseSpec protectedRequest(String token) {
        return client.get().uri("/api/v1/me").headers(h -> h.setBearerAuth(token)).exchange();
    }

    private WebTestClient.ResponseSpec logout(String token, String refresh) {
        // Deliberately unsigned fixture: the provider stub stands in for Keycloak's validation.
        String refreshToken = refresh.isEmpty() ? "" : new PlainJWT(new JWTClaimsSet.Builder()
                .issuer("https://identity.example/realms/matriz").subject("test-user")
                .claim("sid", refresh.equals("other") ? "other-session" : "test-session").build()).serialize();
        return client.post().uri("/api/v1/auth/logout")
                .headers(h -> { if (token != null) h.setBearerAuth(token); })
                .contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("refresh_token", refreshToken)).exchange();
    }

    static Jwt jwt(String token) {
        var builder = Jwt.withTokenValue(token).header("alg", "RS256")
                .issuer("https://identity.example/realms/matriz").subject("test-user")
                .claim("sid", token.equals("other") ? "other-session" : "test-session")
                .issuedAt(Instant.now().minusSeconds(10)).expiresAt(Instant.now().plusSeconds(300));
        if (token.equals("admin")) builder.claim("realm_access", Map.of("roles", List.of("ADMINISTRATOR")));
        return builder.build();
    }

    static class TestState {
        final Map<String, String> values = new ConcurrentHashMap<>();
        final AtomicBoolean redisDown = new AtomicBoolean();
        final AtomicBoolean providerDown = new AtomicBoolean();
        final AtomicBoolean backendDown = new AtomicBoolean();
        final AtomicInteger logoutCalls = new AtomicInteger();
        final AtomicInteger createdUsers = new AtomicInteger();
        final AtomicInteger deletedUsers = new AtomicInteger();
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/v1/me")
        String me() { return "authenticated"; }
    }

    @Configuration
    @EnableWebFlux
    static class TestConfiguration {
        @Bean TestState state() { return new TestState(); }
        @Bean ProtectedController protectedController() { return new ProtectedController(); }
        @Bean ReactiveJwtDecoder jwtDecoder() {
            return token -> token.equals("invalid") ? Mono.error(new BadJwtException("Invalid token"))
                    : Mono.just(jwt(token));
        }
        @Bean
        @SuppressWarnings("unchecked")
        ReactiveStringRedisTemplate redis(TestState state) {
            var redis = mock(ReactiveStringRedisTemplate.class);
            ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
            when(redis.opsForValue()).thenReturn(values);
            when(values.get(anyString())).thenAnswer(call -> Mono.defer(() -> state.redisDown.get()
                    ? Mono.error(new IllegalStateException("Redis unavailable"))
                    : Mono.justOrEmpty(state.values.get(call.getArgument(0)))));
            when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(call ->
                    Mono.defer(() -> state.redisDown.get() ? Mono.error(new IllegalStateException("Redis unavailable"))
                            : Mono.just(state.values.putIfAbsent(call.getArgument(0), call.getArgument(1)) == null)));
            when(values.set(anyString(), anyString(), any(Duration.class))).thenAnswer(call -> Mono.defer(() -> {
                if (state.redisDown.get()) return Mono.error(new IllegalStateException("Redis unavailable"));
                state.values.put(call.getArgument(0), call.getArgument(1));
                return Mono.just(true);
            }));
            return redis;
        }
        @Bean SessionRegistry sessions(ReactiveStringRedisTemplate redis) { return new SessionRegistry(redis); }
        @Bean
        AuthenticationController authenticationController(TestState state, ReactiveJwtDecoder decoder,
                                                          SessionRegistry sessions) {
            var client = WebClient.builder().exchangeFunction(request -> {
                if (request.url().getPath().contains("/admin/") && request.url().getPath().endsWith("/users")
                        && request.method().name().equals("POST")) {
                    state.createdUsers.incrementAndGet();
                    return Mono.just(ClientResponse.create(HttpStatus.CREATED)
                            .header("Location", "https://identity.example/admin/realms/matriz/users/new-user").build());
                }
                if (request.url().getPath().contains("/roles/")) {
                    return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                            .body("{\"id\":\"role-id\",\"name\":\"SOFTWARE_ENGINEER\"}").build());
                }
                if (request.url().getPath().contains("role-mappings/realm")) {
                    return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
                }
                if (request.url().getPath().endsWith("/users/new-user") && request.method().name().equals("DELETE")) {
                    state.deletedUsers.incrementAndGet();
                    return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
                }
                if (request.url().getPath().equals("/api/v1/users")) {
                    return Mono.just(ClientResponse.create(state.backendDown.get() ? HttpStatus.BAD_GATEWAY : HttpStatus.CREATED).header("Content-Type", "application/json")
                            .body("{\"id\":\"4e39c0f2-90b0-44d5-bc8c-968d365d3375\",\"email\":\"new.user@example.com\"}").build());
                }
                if (request.url().getPath().endsWith("/logout")) {
                    state.logoutCalls.incrementAndGet();
                    assertTrue(state.values.containsValue("revoked"), "Revoke before contacting Keycloak");
                    return Mono.just(ClientResponse.create(state.providerDown.get()
                            ? HttpStatus.BAD_GATEWAY : HttpStatus.NO_CONTENT).build());
                }
                return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                        .body("{\"access_token\":\"valid\",\"token_type\":\"Bearer\"}").build());
            }).build();
            return new AuthenticationController(client, client, "matriz", "matriz-client", "matriz-provisioning",
                    "test-provisioning-secret", decoder, sessions);
        }
    }
}
