package com.wikicoding.matrizbackend.apigateway.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Runs outside security so rejected requests are visible too. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class HttpRequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = HttpRequestLoggingFilter.class.getName() + ".id";
    private static final String STATE = HttpRequestLoggingFilter.class.getName() + ".state";

    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var state = (RequestState) request.getAttribute(STATE);
        if (state == null) {
            state = new RequestState(System.nanoTime(), new AtomicBoolean());
            request.setAttribute(STATE, state);
            request.setAttribute(REQUEST_ID, UUID.randomUUID().toString());
        }
        boolean failed = false;
        String previousRequestId = MDC.get("requestId");
        MDC.put("requestId", (String) request.getAttribute(REQUEST_ID));
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            // Exception messages/causes can contain SQL values, credentials or provider bodies.
            log.error("HTTP request failed requestId={} exception={}",
                    request.getAttribute(REQUEST_ID), exception.getClass().getSimpleName());
            throw exception;
        } finally {
            try {
                // MVC Mono results complete on a subsequent ASYNC dispatch, not the initial return.
                if (!request.isAsyncStarted() && state.logged().compareAndSet(false, true)) {
                    Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                    int status = failed ? 500 : response.getStatus();
                    long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - state.started());
                    if (status >= 500 && !failed) {
                        log.error("HTTP server error requestId={} route={} status={} durationMs={}",
                                request.getAttribute(REQUEST_ID), route == null ? "unmapped" : route, status, duration);
                    } else {
                        log.debug("HTTP requestId={} method={} route={} status={} durationMs={}",
                                request.getAttribute(REQUEST_ID), request.getMethod(),
                                route == null ? "unmapped" : route, status, duration);
                    }
                }
            } finally {
                if (previousRequestId == null) {
                    MDC.remove("requestId");
                } else {
                    MDC.put("requestId", previousRequestId);
                }
            }
        }
    }

    private record RequestState(long started, AtomicBoolean logged) {}
}
