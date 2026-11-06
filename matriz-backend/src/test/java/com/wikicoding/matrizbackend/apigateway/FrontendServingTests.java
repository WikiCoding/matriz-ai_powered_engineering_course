package com.wikicoding.matrizbackend.apigateway;

import com.wikicoding.matrizbackend.apigateway.configs.FrontendConfiguration;
import com.wikicoding.matrizbackend.apigateway.configs.SecurityConfiguration;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebAppConfiguration
@SpringJUnitConfig(classes = {SecurityConfiguration.class, FrontendConfiguration.class,
        FrontendServingTests.TestConfiguration.class})
class FrontendServingTests {
    @Autowired WebApplicationContext context;
    MockMvc client;

    @BeforeEach
    void setUp() {
        // Arrange: use the real security chain without a database or identity provider.
        client = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/projects", "/projects/example-id", "/tasks/example-id", "/users", "/teams"})
    void anonymousPageRequestsForwardToReact(String path) throws Exception {
        // Arrange: the page URL is supplied by the parameterized test.
        // Act
        var result = client.perform(get(path));
        // Assert
        result.andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void indexResourceIsPublic() throws Exception {
        // Arrange: a test-only index is served by the configured resource handler.
        // Act
        var result = client.perform(get("/index.html"));
        // Assert
        result.andExpect(status().isOk()).andExpect(content().string(containsString("id=\"root\"")));
    }

    @Test
    void publicAssetSupportsHead() throws Exception {
        // Arrange: a test-only stylesheet is served under the public static path.
        // Act
        var result = client.perform(head("/static/test.css"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(content().contentType("text/css"))
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/projects", "/api/v1/users/me", "/api/v1/teams", "/unmapped"})
    void nonFrontendPathsStillRequireAuthentication(String path) throws Exception {
        // Arrange: no bearer token is supplied.
        // Act
        var result = client.perform(get(path));
        // Assert
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void frontendWriteRequestsRemainProtected() throws Exception {
        // Arrange: public page access applies only to GET and HEAD.
        // Act
        var result = client.perform(post("/projects"));
        // Assert
        result.andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    static class TestConfiguration implements WebMvcConfigurer {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**").addResourceLocations("classpath:/frontend-fixtures/");
        }
    }
}
