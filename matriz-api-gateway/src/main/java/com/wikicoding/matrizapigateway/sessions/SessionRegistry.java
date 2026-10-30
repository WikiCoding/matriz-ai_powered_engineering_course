package com.wikicoding.matrizapigateway.sessions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Missing state is denied, including after Redis data loss. Never cache authorization. */
@Service
@RequiredArgsConstructor
public class SessionRegistry {
    private static final Duration MAX_SESSION = Duration.ofHours(8);
    private final ReactiveStringRedisTemplate redis;

    public Mono<Void> register(Jwt jwt) {
        return Mono.defer(() -> {
            Instant started = jwt.getClaimAsInstant("auth_time");
            if (started == null) started = jwt.getIssuedAt();
            if (started == null) return Mono.error(unauthorized());
            Duration remaining = Duration.between(Instant.now(), started.plus(MAX_SESSION));
            if (remaining.isNegative() || remaining.isZero()) return Mono.error(unauthorized());
            // NX prevents a concurrent login response from undoing logout or extending a session.
            return redis.opsForValue().setIfAbsent(key(jwt), "active", remaining)
                    .then(isActive(jwt))
                    .flatMap(active -> active ? Mono.empty() : Mono.error(unauthorized()));
        }).then().timeout(Duration.ofSeconds(2)).onErrorMap(this::mapFailure);
    }

    public Mono<Boolean> isActive(Jwt jwt) {
        return Mono.defer(() -> redis.opsForValue().get(key(jwt)))
                .map("active"::equals).defaultIfEmpty(false)
                .timeout(Duration.ofSeconds(2)).onErrorMap(this::mapFailure);
    }

    public Mono<Void> revoke(Jwt jwt) {
        return Mono.defer(() -> {
            Duration retention = MAX_SESSION.plusMinutes(1);
            if (jwt.getExpiresAt() != null) {
                Duration tokenRemaining = Duration.between(Instant.now(), jwt.getExpiresAt()).plusMinutes(1);
                if (tokenRemaining.compareTo(retention) > 0) retention = tokenRemaining;
            }
            return redis.opsForValue().set(key(jwt), "revoked", retention)
                    .flatMap(saved -> saved ? Mono.empty()
                            : Mono.error(unavailable()));
        }).then().timeout(Duration.ofSeconds(2)).onErrorMap(this::mapFailure);
    }

    static String key(Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");
        if (sid == null || sid.isBlank() || jwt.getIssuer() == null) throw unauthorized();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (jwt.getIssuer() + "\n" + sid).getBytes(StandardCharsets.UTF_8));
            return "matriz:session:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private Throwable mapFailure(Throwable failure) {
        return failure instanceof ResponseStatusException ? failure : unavailable();
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Session store unavailable");
    }
}
