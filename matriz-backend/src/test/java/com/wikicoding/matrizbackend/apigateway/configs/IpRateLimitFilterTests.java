package com.wikicoding.matrizbackend.apigateway.configs;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import static org.junit.jupiter.api.Assertions.*;

class IpRateLimitFilterTests {
    @Test
    void failedLoginPostsConsumeQuotaAndSixthNeverReachesAuthentication() throws Exception {
        // Arrange
        var time = new java.util.concurrent.atomic.AtomicLong();
        var filter = new IpRateLimitFilter(new UrlBasedCorsConfigurationSource(), "",
                new IpRateLimiter(time::get, 10));
        var authenticationCalls = new AtomicInteger();
        var statuses = new java.util.ArrayList<Integer>();
        jakarta.servlet.FilterChain authentication = (request, response) -> {
            authenticationCalls.incrementAndGet();
            ((jakarta.servlet.http.HttpServletResponse) response).setStatus(401);
        };

        // Act
        for (int attempt = 0; attempt < 6; attempt++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.0.2.1");
            request.setContentType("application/json");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, authentication);
            statuses.add(response.getStatus());
        }
        time.set(1_000_000_000L);
        var retry = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        retry.setRemoteAddr("192.0.2.1");
        var retryResponse = new MockHttpServletResponse();
        filter.doFilter(retry, retryResponse, authentication);

        // Assert
        assertEquals(java.util.List.of(401, 401, 401, 401, 401, 429), statuses);
        assertEquals(6, authenticationCalls.get());
        assertEquals(401, retryResponse.getStatus());
    }

    @Test
    void sharesQuotaAcrossRoutesAndReturnsCorsHeaders() throws Exception {
        // Arrange
        var cors = new UrlBasedCorsConfigurationSource();
        var config = new CorsConfiguration();
        config.addAllowedOrigin("https://app.example.com");
        config.addAllowedMethod("*");
        config.addExposedHeader("Retry-After");
        cors.registerCorsConfiguration("/**", config);
        var filter = new IpRateLimitFilter(cors, "", new IpRateLimiter(() -> 0L, 10));
        var calls = new AtomicInteger();
        MockHttpServletResponse last = null;
        // Act
        for (String path : new String[]{"/api/v1/auth/login", "/actuator/health", "/missing", "/", "/api/v1/users", "/actuator"}) {
            var request = new MockHttpServletRequest("GET", path);
            request.setRemoteAddr("192.0.2.1");
            request.addHeader("Origin", "https://app.example.com");
            last = new MockHttpServletResponse();
            filter.doFilter(request, last, (req, res) -> calls.incrementAndGet());
        }
        // Assert
        assertEquals(5, calls.get());
        assertEquals(429, last.getStatus());
        assertEquals("1", last.getHeader("Retry-After"));
        assertEquals("https://app.example.com", last.getHeader("Access-Control-Allow-Origin"));
        assertTrue(last.getContentAsString().contains("TOO_MANY_REQUESTS"));
    }

    @Test
    void ignoresSpoofedHeadersAndWalksTrustedChainFromRight() {
        // Arrange
        var filter = new IpRateLimitFilter(new UrlBasedCorsConfigurationSource(), "127.0.0.1", 10);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 192.0.2.2");
        // Act
        String direct = filter.clientIp(request);
        request.setRemoteAddr("127.0.0.1");
        String proxied = filter.clientIp(request);
        // Assert
        assertEquals("192.0.2.1", direct);
        assertEquals("192.0.2.2", proxied);
    }
}
