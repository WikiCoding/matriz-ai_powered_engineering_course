> Retired deployment (2026-10-30, D-15): authentication, session validation and IP limiting now run inside `matriz-backend`. This directory is retained as a historical source reference; it is not built or started by Compose. Use [the backend](../matriz-backend/README.md). The remaining text describes the former standalone service.

# Matriz API Gateway

The Matriz API Gateway is the reactive edge service for the Matriz application. It is built with Spring Cloud Gateway Server WebFlux and is responsible for receiving client HTTP requests, applying gateway-level security and rate limiting, and forwarding API traffic to the Matriz backend.

## What it does

- Runs as a reactive HTTP gateway on port `8080` by default.
- Routes requests matching `/**` to the Matriz backend.
- Protects requests with OAuth2 Resource Server JWT validation through Keycloak.
- Exchanges credentials for Keycloak tokens at public `POST /api/v1/auth/login` and its alias `POST /api/v1/login` using the password grant.
- Lets an authenticated Administrator provision an identity and linked local profile at `POST /api/v1/auth/register`.
- Allows configurable CORS origins, methods and request headers across the gateway; the local defaults allow all three and browser credentials are not enabled.
- Applies the Redis-backed `RequestRateLimiter` filter using a client-IP key resolver.
- Exposes health and Prometheus Actuator endpoints.
- Exports Micrometer metrics through Prometheus and optionally through OTLP.

The gateway does not own business data or implement project, task, team, or user-management use cases. Those responsibilities belong to the backend service.

## Connections

The connection targets are configurable through environment variables or the defaults below:

| Target | Configuration | Default |
| --- | --- | --- |
| Matriz backend | `BACKEND_BASE_URL` | `http://localhost:8081` |
| Keycloak issuer | `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/matriz` |
| Redis | `REDIS_HOST`, `REDIS_PORT` | `localhost`, `6379` |
| Prometheus OTLP metrics endpoint | `MANAGEMENT_OTLP_METRICS_EXPORT_URL` | `http://localhost:9090/api/v1/otlp/v1/metrics` |

In the Docker Compose topology, the service is intended to connect to the `backend`, `keycloak`, `redis`, and `prometheus` services over the corresponding internal networks.

## Main dependencies

- Java 21
- Spring Boot 4.1.2
- Spring Cloud Gateway Server WebFlux 2025.1.4
- Spring Security OAuth2 Resource Server
- Spring Data Redis Reactive
- Spring Boot Actuator
- Micrometer Prometheus registry
- Micrometer OTLP registry

## Configuration

The main configuration is in `src/main/resources/application.properties`.

Important settings include:

- `server.port`: listening port, default `8080`.
- `spring.cloud.gateway.server.webflux.routes[0]`: backend route definition.
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`: Keycloak realm used to validate JWTs.
- `keycloak.base-url`, `keycloak.realm` and `keycloak.client-id`: Keycloak token endpoint configuration for the login flow.
- `KEYCLOAK_PROVISIONING_CLIENT_ID` and `KEYCLOAK_PROVISIONING_CLIENT_SECRET`: confidential Keycloak service-account credentials used only for administrator provisioning. The service account needs permission to create users and assign the three Matriz realm roles.
- `BACKEND_BASE_URL`: backend address used to create the application-owned user profile and its required team membership after the Keycloak identity is ready.
- `RATE_LIMIT_REPLENISH_RATE`: tokens added per second, default `5`.
- `RATE_LIMIT_BURST_CAPACITY`: maximum bucket capacity, default `5`.
- `CORS_ALLOWED_ORIGIN_PATTERNS`, `CORS_ALLOWED_METHODS` and `CORS_ALLOWED_HEADERS`: comma-separated CORS allowlists; each defaults to `*` for local development and should be restricted for production.

Health, Prometheus and `POST /api/v1/auth/login` are public. Other application requests require a valid bearer token and an active Redis session registered by gateway login.

## Administrator registration

`POST /api/v1/auth/register` requires a bearer token with the `ADMINISTRATOR` realm role. Its JSON body contains `username`, `email`, `password`, `role`, and `teamId`. The password is sent to Keycloak as temporary and Keycloak requires the new user to change it on first login.

The gateway obtains a service-account token, creates the Keycloak identity, assigns the requested realm role, and calls the backend's `POST /api/v1/users` endpoint with `email`, `role`, and `teamId`. It returns `201` only after both systems succeed. If role assignment or profile creation fails, it deletes the newly created Keycloak identity. This is compensation rather than a distributed transaction; a failed compensation must be reconciled operationally and is never reported as successful provisioning.

The backend user endpoint is not implemented yet, so gateway-level simulated tests cover the authorization, happy path, and Keycloak cleanup after backend failure. Configure the provisioning client and implement the backend endpoint before live use.

## Logout

Send `POST /api/v1/auth/logout` with `Authorization: Bearer <access_token>`, `Content-Type: application/json` and body `{"refresh_token":"<current_refresh_token>"}`. The current public Keycloak client uses its client ID and refresh token to call Keycloak's direct logout endpoint. This is Keycloak's legacy direct logout format, matching the existing password-grant integration; it is not browser RP-initiated logout.

The gateway marks the verified JWT's `sid` as revoked in Redis before calling Keycloak. Success returns `204`. Further API requests using any access token from that session return `401` on all gateway instances sharing Redis. Other sessions remain active. Keycloak failure returns `503` while preserving revocation; retry logout with the same still-valid access token and current refresh token. Missing/blank refresh tokens return `400`.

Session checks run on every authenticated request without an authorization cache. Missing Redis state requires signing in again, including tokens issued before this change or outside gateway login. Redis errors return `503` and deny access. Login registration cannot overwrite a revoked session; active entries expire at eight hours from token `auth_time` (or `iat` when absent). Revocation entries remain for at least eight hours plus clock tolerance, and longer if needed to cover token expiry.

The Compose Redis service uses AOF with `appendfsync always` and no eviction so acknowledged revocations persist before logout succeeds. Recreate Redis with the updated Compose configuration before relying on crash durability; retain its existing volume. External Redis deployments must provide equivalent persistence and must not restore stale active-session snapshots. No running service is restarted by this source change.

This implementation enforces logout in the gateway only, as requested. Backend enforcement and Keycloak-initiated back-channel logout remain unimplemented. Direct backend access must not be exposed as a way around the gateway.

Run regression checks from this directory with `mvn test` (or the Maven wrapper when available). Tests exercise the real security chain and controller with simulated Redis/Keycloak responses; they do not claim a live Keycloak or Redis crash-recovery test.

## Run locally

Start the gateway and its required dependencies. To run the service directly from this directory:

```text
./mvnw spring-boot:run
```

On Windows, use:

```text
.\mvnw.cmd spring-boot:run
```

The gateway needs a reachable backend, Keycloak issuer, and Redis instance for the complete request flow.

## Observability endpoints

- Health: `GET /actuator/health`
- Readiness: `GET /actuator/health/readiness`
- Prometheus metrics: `GET /actuator/prometheus`
- Metrics endpoint: `GET /actuator/metrics`
