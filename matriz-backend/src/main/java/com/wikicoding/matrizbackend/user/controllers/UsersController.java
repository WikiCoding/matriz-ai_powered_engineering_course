package com.wikicoding.matrizbackend.user.controllers;

import com.wikicoding.matrizbackend.user.dtos.RegisterRequest;
import com.wikicoding.matrizbackend.user.dtos.UserItem;
import com.wikicoding.matrizbackend.user.dtos.UserPage;
import com.wikicoding.matrizbackend.user.dtos.KeycloakTokenResponse;
import com.wikicoding.matrizbackend.user.Profile;
import com.wikicoding.matrizbackend.user.UserAccounts;
import com.wikicoding.matrizbackend.user.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
public class UsersController {
    private final UserService users;
    private final WebClient keycloakWebClient;
    private final UserAccounts userService;
    private final String realm;
    private final String provisioningClientId;
    private final String provisioningClientSecret;

    public UsersController(UserService users, @Qualifier("keycloakWebClient") WebClient keycloakWebClient,
            UserAccounts userService,
            @Value("${keycloak.realm:matriz}") String realm,
            @Value("${keycloak.provisioning-client-id:matriz-provisioning}") String provisioningClientId,
            @Value("${keycloak.provisioning-client-secret:}") String provisioningClientSecret) {
        this.users = users;
        this.keycloakWebClient = keycloakWebClient;
        this.userService = userService;
        this.realm = realm;
        this.provisioningClientId = provisioningClientId;
        this.provisioningClientSecret = provisioningClientSecret;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Profile>> register(@AuthenticationPrincipal Jwt administrator,
                                                  @RequestBody RegisterRequest request) {
        if (administrator == null) return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!hasAdministratorRole(administrator)) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
        }
        validateRegistration(request);
        return provisioningToken().flatMap(token -> createKeycloakUser(token, request)
                .flatMap(keycloakUserId -> assignRealmRole(token, keycloakUserId, request.role())
                        .then(createLocalProfile(keycloakUserId, request))
                        .map(profile -> ResponseEntity.status(HttpStatus.CREATED).body(profile))
                        .onErrorResume(failure -> deleteKeycloakUser(token, keycloakUserId)
                                .onErrorResume(deleteFailure -> Mono.empty())
                                .then(Mono.error(asProvisioningFailure(failure))))));
    }

    private Mono<KeycloakTokenResponse> provisioningToken() {
        if (provisioningClientSecret.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Keycloak provisioning client is not configured"));
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", provisioningClientId);
        form.add("client_secret", provisioningClientSecret);
        return keycloakWebClient.post().uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(form))
                .retrieve().bodyToMono(KeycloakTokenResponse.class).onErrorMap(this::asProvisioningFailure);
    }

    private Mono<String> createKeycloakUser(KeycloakTokenResponse token, RegisterRequest request) {
        Map<String, Object> body = Map.of(
                "username", request.username(), "email", request.email(),
                "firstName", request.firstName().trim(), "lastName", request.lastName().trim(),
                "emailVerified", true, "enabled", true,
                "credentials", List.of(Map.of("type", "password", "value", request.password(), "temporary", false)),
                "requiredActions", List.of());
        return keycloakWebClient.post().uri("/admin/realms/{realm}/users", realm)
                .headers(headers -> headers.setBearerAuth(token.access_token()))
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchangeToMono(response -> {
                    if (!response.statusCode().is2xxSuccessful()) return response.createException().flatMap(Mono::error);
                    var location = response.headers().asHttpHeaders().getLocation();
                    if (location == null || location.getPath() == null || !location.getPath().contains("/users/")) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                "Keycloak did not return the created user identifier"));
                    }
                    return Mono.just(location.getPath().substring(location.getPath().lastIndexOf('/') + 1));
                }).onErrorMap(this::asProvisioningFailure);
    }

    private Mono<Void> assignRealmRole(KeycloakTokenResponse token, String keycloakUserId, String role) {
        return keycloakWebClient.get().uri("/admin/realms/{realm}/roles/{role}", realm, role)
                .headers(headers -> headers.setBearerAuth(token.access_token())).retrieve().bodyToMono(String.class)
                .flatMap(roleRepresentation -> keycloakWebClient.post()
                        .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, keycloakUserId)
                        .headers(headers -> headers.setBearerAuth(token.access_token()))
                        .contentType(MediaType.APPLICATION_JSON).bodyValue("[" + roleRepresentation + "]")
                        .retrieve().toBodilessEntity().then()).onErrorMap(this::asProvisioningFailure);
    }

    private Mono<Profile> createLocalProfile(String subject, RegisterRequest request) {
        return Mono.fromCallable(() -> userService.createUser(subject, request.email(), request.teamId()))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<Void> deleteKeycloakUser(KeycloakTokenResponse token, String keycloakUserId) {
        return keycloakWebClient.delete().uri("/admin/realms/{realm}/users/{userId}", realm, keycloakUserId)
                .headers(headers -> headers.setBearerAuth(token.access_token())).retrieve().toBodilessEntity().then();
    }

    private boolean hasAdministratorRole(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access)) return false;
        Object roles = access.get("roles");
        return roles instanceof Collection<?> values && values.contains("ADMINISTRATOR");
    }

    private void validateRegistration(RegisterRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.email())
                || isBlank(request.firstName()) || isBlank(request.lastName()) || isBlank(request.password())
                || request.password().length() < 6 || request.teamId() == null
                || !List.of("ADMINISTRATOR", "PROJECT_MANAGER", "SOFTWARE_ENGINEER").contains(request.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "username, email, firstName, lastName, password, role and teamId are required");
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private Throwable asProvisioningFailure(Throwable failure) {
        if (failure instanceof ResponseStatusException) return failure;
        if (failure instanceof org.springframework.dao.DataIntegrityViolationException) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                        && "app_user_team_id_fkey".equals(constraint.getConstraintName())) {
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "teamId does not reference an existing team");
                }
            }
            return new ResponseStatusException(HttpStatus.CONFLICT, "Profile conflicts with existing data or team membership");
        }
        if (failure instanceof WebClientResponseException response) {
            if (response.getStatusCode().is5xxServerError()) {
                return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User provisioning dependency failed");
            }
            return new ResponseStatusException(response.getStatusCode(), "User provisioning failed");
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User provisioning failed");
    }

    @GetMapping
    public UserPage findAllUsers(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        var result = users.findAllUsersPagedAndSorted(page, size);
        return new UserPage(result.getContent().stream()
                .map(user -> new UserItem(user.id(), user.email(), user.teamId())).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

}