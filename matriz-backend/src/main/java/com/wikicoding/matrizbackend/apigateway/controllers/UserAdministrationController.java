package com.wikicoding.matrizbackend.apigateway.controllers;

import com.wikicoding.matrizbackend.apigateway.dtos.KeycloakTokenResponse;
import com.wikicoding.matrizbackend.apigateway.dtos.RoleRequest;
import com.wikicoding.matrizbackend.apigateway.dtos.TeamIds;
import com.wikicoding.matrizbackend.apigateway.dtos.UserRoleResponse;
import com.wikicoding.matrizbackend.user.UserManagement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users/{userId}")
public class UserAdministrationController {
    private static final List<String> ROLES = List.of("ADMINISTRATOR", "PROJECT_MANAGER", "SOFTWARE_ENGINEER");
    private final UserManagement users;
    private final WebClient keycloak;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public UserAdministrationController(UserManagement users,
            @Qualifier("keycloakWebClient") WebClient keycloak,
            @Value("${keycloak.realm:matriz}") String realm,
            @Value("${keycloak.provisioning-client-id:matriz-provisioning}") String clientId,
            @Value("${keycloak.provisioning-client-secret:}") String clientSecret) {
        this.users = users;
        this.keycloak = keycloak;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @GetMapping("/team-memberships")
    public TeamIds membership(@PathVariable UUID userId) {
        return new TeamIds(List.of(users.requireActive(userId).teamId()));
    }

    @PutMapping("/team-memberships")
    public TeamIds move(@PathVariable UUID userId, @RequestBody TeamIds request) {
        if (request == null || request.teamIds() == null || request.teamIds().size() != 1
                || request.teamIds().getFirst() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one team is required");
        }
        UUID teamId = request.teamIds().getFirst();
        users.moveToTeam(userId, teamId);
        return new TeamIds(List.of(teamId));
    }

    @PutMapping("/role")
    public UserRoleResponse role(@PathVariable UUID userId, @RequestBody RoleRequest request) {
        if (request == null || !ROLES.contains(request.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one valid role is required");
        }
        var user = users.requireActive(userId);
        String token = token();
        try {
            var type = new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {};
            List<Map<String, Object>> current = keycloak.get()
                    .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, user.subject())
                    .headers(h -> h.setBearerAuth(token)).retrieve().bodyToMono(type)
                    .block(Duration.ofSeconds(5));
            List<Map<String, Object>> managed = current == null ? List.of() : current.stream()
                    .filter(item -> ROLES.contains(item.get("name"))).toList();
            if (!managed.isEmpty()) keycloak.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, user.subject())
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(managed).retrieve().toBodilessEntity().block(Duration.ofSeconds(5));
            Map<String, Object> replacement = keycloak.get()
                    .uri("/admin/realms/{realm}/roles/{role}", realm, request.role())
                    .headers(h -> h.setBearerAuth(token)).retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(5));
            keycloak.post().uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, user.subject())
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(List.of(replacement)).retrieve().toBodilessEntity().block(Duration.ofSeconds(5));
            if (!"SOFTWARE_ENGINEER".equals(request.role())) users.removeAssignments(userId);
            return new UserRoleResponse(user.id(), user.email(), request.role());
        } catch (RuntimeException failure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Role change may require reconciliation", failure);
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable UUID userId) {
        var user = users.requireActive(userId);
        String token = token();
        try {
            try {
                keycloak.delete().uri("/admin/realms/{realm}/users/{subject}", realm, user.subject())
                        .headers(h -> h.setBearerAuth(token)).retrieve().toBodilessEntity().block(Duration.ofSeconds(5));
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound alreadyDeleted) {
                // A previous attempt may have removed the identity before local cleanup failed.
            }
            users.deleteLocal(userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException failure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Account deletion may require reconciliation", failure);
        }
    }

    private String token() {
        if (clientSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Keycloak provisioning client is not configured");
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        try {
            KeycloakTokenResponse response = keycloak.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form)).retrieve()
                    .bodyToMono(KeycloakTokenResponse.class).block(Duration.ofSeconds(5));
            return response.access_token();
        } catch (RuntimeException failure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Identity provider unavailable", failure);
        }
    }

}
