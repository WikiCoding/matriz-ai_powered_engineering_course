package com.wikicoding.matrizbackend.apigateway.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class IpRateLimitFilter extends OncePerRequestFilter {
    private final IpRateLimiter limiter;
    private final Set<String> trustedProxies;
    private final UrlBasedCorsConfigurationSource cors;

    @org.springframework.beans.factory.annotation.Autowired
    public IpRateLimitFilter(UrlBasedCorsConfigurationSource cors,
            @Value("${app.rate-limit.trusted-proxies:}") String proxies,
            @Value("${app.rate-limit.max-ips:10000}") int capacity) {
        this(cors, proxies, new IpRateLimiter(System::nanoTime, capacity));
    }

    IpRateLimitFilter(UrlBasedCorsConfigurationSource cors, String proxies, IpRateLimiter limiter) {
        this.cors = cors;
        this.limiter = limiter;
        trustedProxies = Arrays.stream(proxies.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).map(IpRateLimitFilter::address)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String address(String value) {
        // Numeric literals only: never resolve a caller-controlled hostname.
        if (!value.matches("[0-9.]+") && !(value.contains(":") && value.matches("[0-9a-fA-F:.]+"))) {
            throw new IllegalArgumentException("Expected an IP literal");
        }
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Invalid IP literal", exception);
        }
    }

    String clientIp(HttpServletRequest request) {
        String peer = address(request.getRemoteAddr());
        String forwarded = request.getHeader("X-Forwarded-For");
        if (!trustedProxies.contains(peer) || forwarded == null) return peer;
        String current = peer;
        String[] chain = forwarded.split(",", -1);
        try {
            for (int index = chain.length - 1; index >= 0 && trustedProxies.contains(current); index--) {
                current = address(chain[index].trim());
            }
            return current;
        } catch (IllegalArgumentException exception) {
            return peer;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (limiter.acquire(clientIp(request))) {
            chain.doFilter(request, response);
            return;
        }
        new DefaultCorsProcessor().processRequest(cors.getCorsConfiguration(request), request, response);
        response.resetBuffer();
        response.setStatus(429);
        response.setHeader("Retry-After", "1");
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"IP request limit reached; retry after one second\"}");
    }
}
