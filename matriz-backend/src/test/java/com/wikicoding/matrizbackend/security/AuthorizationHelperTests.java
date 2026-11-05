package com.wikicoding.matrizbackend.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationHelperTests {
    @ParameterizedTest
    @MethodSource("invalidClaims")
    void malformedClaimsDoNotGrantRoles(Object realmAccess) {
        // Arrange
        var builder = Jwt.withTokenValue("test").header("alg", "RS256").subject("test-user");
        if (realmAccess != null) builder.claim("realm_access", realmAccess);
        var jwt = builder.build();

        // Act
        var roles = AuthorizationHelper.extractRoles(jwt);
        boolean administrator = AuthorizationHelper.hasRole(jwt, "ADMINISTRATOR");

        // Assert
        assertThat(roles).isEmpty();
        assertThat(administrator).isFalse();
    }

    static Stream<Object> invalidClaims() {
        return Stream.of(null, "ADMINISTRATOR", Map.of(), Map.of("roles", "ADMINISTRATOR"),
                Map.of("roles", Arrays.asList(null, 42, Map.of("name", "ADMINISTRATOR"))));
    }

    @ParameterizedTest
    @MethodSource("validRoles")
    void keepsOnlyStringRoles(List<?> claim) {
        // Arrange
        var jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("test-user")
                .claim("realm_access", Map.of("roles", claim)).build();

        // Act
        var roles = AuthorizationHelper.extractRoles(jwt);

        // Assert
        assertThat(roles).containsExactly("PROJECT_MANAGER");
    }

    static Stream<List<?>> validRoles() {
        return Stream.of(List.of("PROJECT_MANAGER"), Arrays.asList(null, 42, "PROJECT_MANAGER"));
    }
}
