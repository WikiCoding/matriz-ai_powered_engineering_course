package com.wikicoding.matrizbackend.edge.configs;

import com.wikicoding.matrizbackend.apigateway.configs.SecurityConfiguration;
import jakarta.servlet.Filter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@SpringJUnitConfig(classes = {SecurityConfiguration.class, AuthorizationRoutingTests.TestConfiguration.class})
class AuthorizationRoutingTests {
    @Autowired WebApplicationContext context;
    WebTestClient client;

    @BeforeEach
    void setUp() {
        // Arrange: use the actual security filter chain with a stub identity provider and endpoint.
        client = MockMvcWebTestClient.bindToApplicationContext(context)
                .filter(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @ParameterizedTest(name = "{0} {1} as {2} returns {3}")
    @MethodSource("permissions")
    void enforcesRoleAndMethodPermissions(String method, String path, String role, int expectedStatus) {
        // Arrange
        var request = client.method(HttpMethod.valueOf(method)).uri(path);
        if (!role.equals("anonymous")) request.headers(headers -> headers.setBearerAuth(role));

        // Act / Assert
        request.exchange().expectStatus().isEqualTo(expectedStatus);
    }

    static Stream<Arguments> permissions() {
        var comments = List.of("/api/v1/comments", "/api/v1/comments/comment-id", "/api/v1/tasks/task-id/comments");
        var work = List.of("/api/v1/projects", "/api/v1/projects/project-id", "/api/v1/projects/project-id/tasks",
                "/api/v1/tasks", "/api/v1/tasks/task-id", "/api/v1/tasks/task-id/assignments",
                "/api/v1/tasks/task-id/assignments/user-id");
        return Stream.concat(comments.stream(), work.stream()).flatMap(path ->
                Stream.of("GET", "POST", "PUT", "PATCH", "DELETE").flatMap(method ->
                        Stream.of("PROJECT_MANAGER", "SOFTWARE_ENGINEER", "ADMINISTRATOR", "no-role", "malformed", "anonymous")
                                .map(role -> {
                                    boolean manager = role.equals("PROJECT_MANAGER");
                                    boolean engineer = role.equals("SOFTWARE_ENGINEER");
                                    boolean allowed = comments.contains(path)
                                            ? (method.equals("GET") && (manager || engineer))
                                                || (method.equals("POST") && engineer)
                                            : manager || (method.equals("GET") && engineer);
                                    return Arguments.of(method, path, role,
                                            role.equals("anonymous") ? 401 : allowed ? 200 : 403);
                                })));
    }

    @RestController
    static class Endpoint {
        // Support all methods so a missing MVC mapping cannot hide a filter authorization regression.
        @RequestMapping("/api/v1/**")
        String allowed() { return "allowed"; }
    }

    @Configuration
    @EnableWebMvc
    static class TestConfiguration {
        @Bean Endpoint endpoint() { return new Endpoint(); }
        @Bean JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token).header("alg", "RS256").subject("test-user")
                    .claim("realm_access", Map.of("roles", token.equals("malformed")
                            ? Arrays.asList(null, 42) : List.of(token)))
                    .build();
        }
    }
}
