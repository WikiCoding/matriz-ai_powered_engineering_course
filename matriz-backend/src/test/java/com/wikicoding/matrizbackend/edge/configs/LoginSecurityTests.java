package com.wikicoding.matrizbackend.edge.configs;


import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.wikicoding.matrizbackend.apigateway.configs.SecurityConfiguration;
import com.wikicoding.matrizbackend.apigateway.controllers.AuthenticationController;
import com.wikicoding.matrizbackend.user.controllers.UsersController;
import com.wikicoding.matrizbackend.user.Profile;
import com.wikicoding.matrizbackend.user.UserAccounts;
import com.wikicoding.matrizbackend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@SpringJUnitConfig(classes = {SecurityConfiguration.class, LoginSecurityTests.TestConfiguration.class})
@TestPropertySource(properties = {
        "cors.allowed-origin-patterns=https://app.example.com",
        "cors.allowed-methods=GET,POST",
        "cors.allowed-headers=Authorization,Content-Type"
})
class LoginSecurityTests {
    @Autowired WebApplicationContext context;
    @Autowired TestState state;
    WebTestClient client;

    @Test
    void corsConfigurationUsesConfiguredValues() {
        // Arrange: fixtures and dependency state are reset in resetState().
        var source = context.getBean("corsConfigurationSource", CorsConfigurationSource.class);
        var exchange = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/me");
        // Act
        var cors = source.getCorsConfiguration(exchange);
        // Assert
        assertNotNull(cors);

        assertEquals(List.of("https://app.example.com"), cors.getAllowedOriginPatterns());
        assertEquals(List.of("GET", "POST"), cors.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type"), cors.getAllowedHeaders());
    }

    @BeforeEach
    void resetState() {
        // Arrange: shared isolated test state.
        state.providerDown.set(false);
        state.backendDown.set(false);
        state.logoutCalls.set(0);
        state.createdUsers.set(0);
        state.deletedUsers.set(0);
        client = MockMvcWebTestClient.bindToApplicationContext(context)
                .filter(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }

    @Test
    void loginAcceptsAnonymousCredentials() {
        // Arrange: fixtures and dependency state are reset in resetState().
        // Act / Assert: execute the workflow and verify responses/state.
        client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"test\",\"password\":\"test\"}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.access_token").isEqualTo("valid");
        protectedRequest("valid").expectStatus().isOk();
    }

    @Test
    void administratorRegistrationCreatesIdentityRoleAndLocalProfile() {
        // Arrange: fixtures and dependency state are reset in resetState().
        // Act / Assert: execute the workflow and verify responses/state.
        client.post().uri("/api/v1/users").headers(h -> h.setBearerAuth("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"new.user\",\"email\":\"new.user@example.com\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"a-long-temporary-password\",\"role\":\"SOFTWARE_ENGINEER\",\"teamId\":\"4e39c0f2-90b0-44d5-bc8c-968d365d3375\"}")
                .exchange().expectStatus().isCreated().expectBody().jsonPath("$.email").isEqualTo("new.user@example.com");
        assertEquals(1, state.createdUsers.get());
        assertEquals(0, state.deletedUsers.get());
    }

    @Test
    void registrationRejectsNonAdministrators() {
        // Arrange: fixtures and dependency state are reset in resetState().
        // Act / Assert: execute the workflow and verify responses/state.
        client.post().uri("/api/v1/users").headers(h -> h.setBearerAuth("valid"))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange().expectStatus().isForbidden();
    }

    @Test
    void userAdministrationRejectsNonAdministrators() {
        // Arrange
        String userId = "4e39c0f2-90b0-44d5-bc8c-968d365d3375";

        // Act / Assert
        client.put().uri("/api/v1/users/" + userId + "/role")
                .headers(h -> h.setBearerAuth("valid"))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"role\":\"PROJECT_MANAGER\"}")
                .exchange().expectStatus().isForbidden();
        client.delete().uri("/api/v1/users/" + userId)
                .headers(h -> h.setBearerAuth("valid"))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void registrationRejectsMissingNamesBeforeCreatingIdentity() {
        // Arrange
        String request = "{\"username\":\"new.user\",\"email\":\"new.user@example.com\",\"firstName\":\" \",\"password\":\"a-long-temporary-password\",\"role\":\"SOFTWARE_ENGINEER\",\"teamId\":\"4e39c0f2-90b0-44d5-bc8c-968d365d3375\"}";
        // Act
        client.post().uri("/api/v1/users").headers(h -> h.setBearerAuth("admin"))
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isBadRequest();
        // Assert
        assertEquals(0, state.createdUsers.get());
    }

    @Test
    void registrationDeletesKeycloakIdentityWhenLocalProfileCreationFails() {
        // Arrange: fixtures and dependency state are reset in resetState().
        state.backendDown.set(true);
        // Act / Assert: execute the workflow and verify responses/state.
        client.post().uri("/api/v1/users").headers(h -> h.setBearerAuth("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"new.user\",\"email\":\"new.user@example.com\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"a-long-temporary-password\",\"role\":\"SOFTWARE_ENGINEER\",\"teamId\":\"4e39c0f2-90b0-44d5-bc8c-968d365d3375\"}")
                .exchange().expectStatus().isEqualTo(502);
        assertEquals(1, state.deletedUsers.get());
    }

    @Test
    void protectedRoutesAndLogoutRequireAuthentication() {
        // Arrange: fixtures and dependency state are reset in resetState().
        // Act / Assert: execute the workflow and verify responses/state.
        client.get().uri("/api/v1/me").exchange().expectStatus().isUnauthorized();
        logout(null, "refresh").expectStatus().isUnauthorized();
        logout("invalid", "refresh").expectStatus().isUnauthorized();
        assertEquals(0, state.logoutCalls.get());
    }

    @Test
    void teamRoutesRequireAdministratorAtEdge() {
        // Arrange
        var teamPath = "/api/v1/teams/" + java.util.UUID.randomUUID();

        // Act / Assert
        client.get().uri("/api/v1/teams").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/v1/teams").headers(h -> h.setBearerAuth("valid"))
                .exchange().expectStatus().isForbidden();
        client.delete().uri(teamPath).headers(h -> h.setBearerAuth("valid"))
                .exchange().expectStatus().isForbidden();
        client.get().uri("/api/v1/teams").headers(h -> h.setBearerAuth("admin"))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void logoutLeavesIssuedTokensUsableUntilExpiry() {
        // Arrange: provider succeeds and tokens are valid.
        // Act
        logout("valid", "refresh").expectStatus().isNoContent();
        // Assert
        protectedRequest("valid").expectStatus().isOk();
        protectedRequest("renewed").expectStatus().isOk();
        assertEquals(1, state.logoutCalls.get());
    }

    @Test
    void logoutDoesNotRevokeAnotherSession() {
        // Arrange: fixtures and dependency state are reset in resetState().
        // Act / Assert: execute the workflow and verify responses/state.
        logout("valid", "refresh").expectStatus().isNoContent();
        protectedRequest("other").expectStatus().isOk();
    }

    @Test
    void keycloakFailureReportsFailureAndAllowsLogoutRetry() {
        // Arrange: fixtures and dependency state are reset in resetState().
        state.providerDown.set(true);
        // Act / Assert: execute the workflow and verify responses/state.
        logout("valid", "refresh").expectStatus().isEqualTo(503);
        protectedRequest("valid").expectStatus().isOk();
        state.providerDown.set(false);
        logout("valid", "refresh").expectStatus().isNoContent();
        protectedRequest("valid").expectStatus().isOk();
    }




    @Test
    void logoutRequiresRefreshToken() {
        // Arrange: fixtures and dependency state are reset in resetState().
        // Act / Assert: execute the workflow and verify responses/state.
        logout("valid", "").expectStatus().isBadRequest();
        protectedRequest("valid").expectStatus().isOk();
        assertEquals(0, state.logoutCalls.get());
    }

    @Test
    void logoutRejectsRefreshTokenFromAnotherSession() {
        // Arrange: fixtures and dependency state are reset in resetState().
        // Act / Assert: execute the workflow and verify responses/state.
        logout("valid", "other").expectStatus().isBadRequest();
        protectedRequest("valid").expectStatus().isOk();
        assertEquals(0, state.logoutCalls.get());
    }



    @Test
    void invalidLoginInputIsRejected() {
        // Arrange
        var input = Map.of("username", "", "password", "");
        // Act / Assert
        client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(input).exchange().expectStatus().isBadRequest();
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
    @EnableWebMvc
    static class TestConfiguration {
        @Bean TestState state() { return new TestState(); }
        @Bean ProtectedController protectedController() { return new ProtectedController(); }
        @Bean JwtDecoder jwtDecoder() {
            return token -> { if (token.equals("invalid")) throw new BadJwtException("Invalid token"); return jwt(token); };
        }
        @Bean
        WebClient keycloakWebClient(TestState state) {
            return WebClient.builder().exchangeFunction(request -> {
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
                if (request.url().getPath().endsWith("/logout")) {
                    state.logoutCalls.incrementAndGet();
                    return Mono.just(ClientResponse.create(state.providerDown.get()
                            ? HttpStatus.BAD_GATEWAY : HttpStatus.NO_CONTENT).build());
                }
                return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                        .body("{\"access_token\":\"valid\",\"token_type\":\"Bearer\"}").build());
            }).build();
        }
        @Bean
        UserAccounts userAccounts(TestState state) {
            var profiles = mock(UserAccounts.class);
            when(profiles.createUser(anyString(), anyString(), any())).thenAnswer(call -> {
                if (state.backendDown.get()) throw new IllegalStateException("Local persistence unavailable");
                return new Profile(java.util.UUID.randomUUID(), "new.user@example.com");
            });
            return profiles;
        }
        @Bean
        AuthenticationController authenticationController(WebClient keycloakWebClient, ReactiveJwtDecoder decoder) {
            return new AuthenticationController(keycloakWebClient, "matriz", "matriz-client", decoder);
        }
        @Bean
        UserService userService() { return mock(UserService.class); }
        @Bean
        UsersController usersController(UserService userService, WebClient keycloakWebClient, UserAccounts userAccounts) {
            return new UsersController(userService, keycloakWebClient, userAccounts, "matriz", "matriz-provisioning",
                    "test-provisioning-secret");
        }
    }
}
