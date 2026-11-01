package com.wikicoding.matrizbackend.user.services;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wikicoding.matrizbackend.user.UserEligibility;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KeycloakUserEligibility implements UserEligibility {
    private final UserService userService;
    private final WebClient keycloakClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakUserEligibility(UserService userService, @Qualifier("keycloakWebClient") WebClient keycloakClient,
                                   @Value("${keycloak.realm:matriz}") String realm,
                                   @Value("${keycloak.provisioning-client-id:matriz-provisioning}") String clientId,
                                   @Value("${keycloak.provisioning-client-secret:}") String clientSecret) {
        this.userService = userService;
        this.keycloakClient = keycloakClient;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public void requireSoftwareEngineerInTeam(UUID userId, UUID teamId) {
        var user = userService.requireActive(userId);

        if (!teamId.equals(user.teamId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must belong to the project team");
        }

        if (clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity provider is not configured");
        }

        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            Map<String, Object> token = keycloakClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form)).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}).block(Duration.ofSeconds(5));
            if (token == null || !(token.get("access_token") instanceof String accessToken)) {
                throw new IllegalStateException("Missing service token");
            }
            List<Map<String, Object>> roles = keycloakClient.get()
                    .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, user.subject())
                    .headers(headers -> headers.setBearerAuth(accessToken)).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block(Duration.ofSeconds(5));
            if (roles == null || roles.stream().noneMatch(role -> "SOFTWARE_ENGINEER".equals(role.get("name")))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must be a Software Engineer");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity provider unavailable", exception);
        }
    }
}
