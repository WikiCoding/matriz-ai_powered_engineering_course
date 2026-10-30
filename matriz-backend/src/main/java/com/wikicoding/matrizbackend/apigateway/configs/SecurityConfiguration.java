package com.wikicoding.matrizbackend.edge.configs;

import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private static List<String> csv(String value) {
        return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(JwtDecoder decoder) {
        return token -> Mono.fromCallable(() -> decoder.decode(token)).subscribeOn(Schedulers.boundedElastic());
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origin-patterns:*}") String origins,
            @Value("${cors.allowed-methods:*}") String methods,
            @Value("${cors.allowed-headers:*}") String headers) {
        var cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(csv(origins));
        cors.setAllowedMethods(csv(methods));
        cors.setAllowedHeaders(csv(headers));
        cors.setExposedHeaders(List.of("Location"));
        cors.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .cors(cors -> {})
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
                .build();
    }
}
