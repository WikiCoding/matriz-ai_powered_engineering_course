package com.wikicoding.matrizapigateway.configs;

import com.wikicoding.matrizapigateway.security.SessionRegistry;
import com.wikicoding.matrizapigateway.security.SessionValidationFilter;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of(Arrays.toString(HttpMethod.values())));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(false);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    CorsWebFilter corsWebFilter(CorsConfigurationSource source) {
        return new CorsWebFilter(source);
    }

    @Bean(name = "clientIpKeyResolver")
    KeyResolver clientIpKeyResolver() {
        return exchange -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress == null || remoteAddress.getAddress() == null) {
                return Mono.just("unknown");
            }
            return Mono.just(remoteAddress.getAddress().getHostAddress());
        };
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, SessionRegistry sessions) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyExchange().authenticated())
                .cors(cors -> { })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> { }))
                .addFilterAfter(new SessionValidationFilter(sessions), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
