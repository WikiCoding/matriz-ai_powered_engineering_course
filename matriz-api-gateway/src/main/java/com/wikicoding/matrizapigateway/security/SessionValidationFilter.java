package com.wikicoding.matrizapigateway.security;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public class SessionValidationFilter implements WebFilter {
    private final SessionRegistry sessions;

    public SessionValidationFilter(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // A signed, unexpired JWT may retry logout after Keycloak failed. No business access is granted.
        if (exchange.getRequest().getMethod() == HttpMethod.POST
                && exchange.getRequest().getPath().value().equals("/api/v1/auth/logout")) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext().mapNotNull(SecurityContext::getAuthentication)
                .ofType(JwtAuthenticationToken.class)
                .flatMap(authentication -> sessions.isActive(authentication.getToken()))
                .defaultIfEmpty(true)
                .flatMap(active -> {
                    if (active) return chain.filter(exchange);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(ResponseStatusException.class, failure -> {
                    exchange.getResponse().setStatusCode(failure.getStatusCode());
                    return exchange.getResponse().setComplete();
                });
    }
}
