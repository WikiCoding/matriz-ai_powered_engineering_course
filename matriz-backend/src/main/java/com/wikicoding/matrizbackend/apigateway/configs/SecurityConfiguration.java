package com.wikicoding.matrizbackend.apigateway.configs;

import com.wikicoding.matrizbackend.security.AuthorizationHelper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfiguration {
    private static List<String> csv(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
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
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(requestLoggingFilter(), AnonymousAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, FrontendConfiguration.PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.HEAD, FrontendConfiguration.PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/display")
                        .access(AuthorizationHelper.hasAnyRole("PROJECT_MANAGER", "SOFTWARE_ENGINEER"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/teams", "/api/v1/teams/*")
                        .access(AuthorizationHelper.hasAnyRole("ADMINISTRATOR", "PROJECT_MANAGER", "SOFTWARE_ENGINEER"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/*/assignee-options")
                        .access(AuthorizationHelper.hasAnyRole("PROJECT_MANAGER"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/dashboard")
                        .access(AuthorizationHelper.hasAnyRole("ADMINISTRATOR", "PROJECT_MANAGER", "SOFTWARE_ENGINEER"))
                        .requestMatchers("/api/v1/teams", "/api/v1/teams/**", "/api/v1/users", "/api/v1/users/**")
                        .access(AuthorizationHelper.hasAnyRole("ADMINISTRATOR"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/comments", "/api/v1/comments/*", "/api/v1/tasks/*/comments")
                        .access(AuthorizationHelper.hasAnyRole("PROJECT_MANAGER", "SOFTWARE_ENGINEER"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/comments", "/api/v1/comments/*", "/api/v1/tasks/*/comments")
                        .access(AuthorizationHelper.hasAnyRole("SOFTWARE_ENGINEER"))
                        .requestMatchers("/api/v1/comments", "/api/v1/comments/*", "/api/v1/tasks/*/comments")
                        .denyAll()
                        .requestMatchers("/api/v1/tasks/*/assignments", "/api/v1/tasks/*/assignments/*")
                        .access(AuthorizationHelper.hasRoleForMethod(
                                HttpMethod.GET.name(),
                                new String[]{"PROJECT_MANAGER", "SOFTWARE_ENGINEER"},
                                new String[]{"PROJECT_MANAGER"}))
                        .requestMatchers("/api/v1/projects", "/api/v1/projects/*")
                        .access(AuthorizationHelper.hasRoleForMethod(
                                HttpMethod.GET.name(),
                                new String[]{"PROJECT_MANAGER", "SOFTWARE_ENGINEER"},
                                new String[]{"PROJECT_MANAGER"}))
                        .requestMatchers("/api/v1/projects/*/tasks", "/api/v1/tasks", "/api/v1/tasks/*")
                        .access(AuthorizationHelper.hasRoleForMethod(
                                HttpMethod.GET.name(),
                                new String[]{"PROJECT_MANAGER", "SOFTWARE_ENGINEER"},
                                new String[]{"PROJECT_MANAGER"}))
                        .anyRequest().authenticated())
                .cors(cors -> {})
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
                .build();
    }

    private OncePerRequestFilter requestLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                            @NonNull FilterChain chain) throws ServletException, IOException {
                try {
                    chain.doFilter(request, response);
                } finally {
                    log.info("HTTP {} {} from {} -> {}", request.getMethod(), request.getRequestURI(),
                            request.getRemoteAddr(), response.getStatus());
                }
            }
        };
    }
}
