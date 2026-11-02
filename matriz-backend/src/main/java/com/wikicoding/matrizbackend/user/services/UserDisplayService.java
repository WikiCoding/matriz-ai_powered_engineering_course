package com.wikicoding.matrizbackend.user.services;

import com.wikicoding.matrizbackend.projectmanagement.AssigneeOptionsLookup;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wikicoding.matrizbackend.user.dtos.Display;
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
public class UserDisplayService implements AssigneeOptionsLookup {
    private final UserService users;
    private final WebClient keycloak;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public UserDisplayService(UserService users, @Qualifier("keycloakWebClient") WebClient keycloak,
            @Value("${keycloak.realm:matriz}") String realm,
            @Value("${keycloak.provisioning-client-id:matriz-provisioning}") String clientId,
            @Value("${keycloak.provisioning-client-secret:}") String clientSecret) {
        this.users = users;
        this.keycloak = keycloak;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Display display(UUID userId) {
        var user = users.requireActive(userId);
        try {
            Map<String, Object> identity = keycloak.get()
                    .uri("/admin/realms/{realm}/users/{subject}", realm, user.subject())
                    .headers(headers -> headers.setBearerAuth(token())).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(5));
            if (identity == null) throw new IllegalStateException("Identity not found");
            return new Display(user.id(), name(identity, user.email()), user.email());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity provider unavailable", exception);
        }
    }

    @Override
    public List<Option> eligibleForTeam(UUID teamId) {
        var members = users.findActiveTeamMembers(teamId);
        if (members.isEmpty()) return List.of();
        try {
            String accessToken = token();
            List<Map<String, Object>> engineers = keycloak.get()
                    .uri(uri -> uri.path("/admin/realms/{realm}/roles/SOFTWARE_ENGINEER/users")
                            .queryParam("first", 0).queryParam("max", 1000).build(realm))
                    .headers(headers -> headers.setBearerAuth(accessToken)).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block(Duration.ofSeconds(5));
            if (engineers == null) throw new IllegalStateException("Role members unavailable");
            Map<String, Map<String, Object>> bySubject = new java.util.HashMap<>();
            for (var engineer : engineers) {
                if (engineer.get("id") instanceof String subject) bySubject.put(subject, engineer);
            }
            return members.stream().filter(member -> bySubject.containsKey(member.subject()))
                    .map(member -> new Option(member.id(), name(bySubject.get(member.subject()), member.email())))
                    .sorted(Comparator.comparing(Option::displayName).thenComparing(Option::id)).toList();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity provider unavailable", exception);
        }
    }

    private String token() {
        if (clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity provider is not configured");
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<String, Object> response = keycloak.post().uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form)).retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(5));
        if (response == null || !(response.get("access_token") instanceof String value)) {
            throw new IllegalStateException("Service token unavailable");
        }
        return value;
    }

    private String name(Map<String, Object> identity, String fallback) {
        String first = identity.get("firstName") instanceof String value ? value.trim() : "";
        String last = identity.get("lastName") instanceof String value ? value.trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? fallback : full;
    }
}
