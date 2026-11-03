package com.wikicoding.matrizbackend.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

public final class AuthorizationHelper {

    private AuthorizationHelper() {}

    public static Collection<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access)) return java.util.Collections.emptyList();
        Object roles = access.get("roles");
        if (!(roles instanceof Collection<?> values)) return java.util.Collections.emptyList();
        return values.stream().map(Object::toString).toList();
    }

    public static boolean hasRole(Jwt jwt, String role) {
        return extractRoles(jwt).contains(role);
    }

    public static AuthorizationManager<RequestAuthorizationContext> hasAnyRole(String... roles) {
        java.util.Set<String> allowed = java.util.Set.of(roles);
        return (authentication, context) -> {
            var principal = authentication.get();
            boolean has = principal instanceof JwtAuthenticationToken jwt
                    && extractRoles(jwt.getToken()).stream().anyMatch(allowed::contains);
            return new AuthorizationDecision(has);
        };
    }

    public static AuthorizationManager<RequestAuthorizationContext> hasRoleForMethod(
            String method, String[] methodRoles, String[] fallbackRoles) {
        java.util.Set<String> allowedMethod = java.util.Set.of(methodRoles);
        java.util.Set<String> fallback = java.util.Set.of(fallbackRoles);
        return (authentication, context) -> {
            var principal = authentication.get();
            boolean has = principal instanceof JwtAuthenticationToken jwt
                    && extractRoles(jwt.getToken()).stream()
                            .anyMatch(m -> m.equals(method)
                                    ? allowedMethod.contains(m)
                                    : fallback.contains(m));
            return new AuthorizationDecision(has);
        };
    }
}
