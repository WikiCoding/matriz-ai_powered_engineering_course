# Backend scaffold

The backend has three closed Spring Modulith modules under `com.wikicoding.matrizbackend`: `user`, `team`, and `projectmanagement`. Public types in each module root form its API. Nested packages are internal; use explicit module interfaces for cross-module access.

Each module contains documented packages for `domain`, `application`, `adapter.in.web`, `adapter.out.persistence`, and `configuration`. Project Management has separate domain packages for projects, tasks, assignments, and comments. These are package scaffolds, not implemented use cases or entities. Domain code must remain independent of Spring and persistence frameworks.

## PostgreSQL configuration

The backend uses Spring Data JPA. `application.properties` uses `DB_URL` for the complete PostgreSQL connection URL, defaulting to `jdbc:postgresql://localhost:5432/matriz`, and `APP_DB_USER` (default `matriz`). Set `APP_DB_PASSWORD` to the password configured for the Compose `app-db` service. No password is embedded in the backend configuration.

Standard Spring overrides `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` also work. For a backend running inside Compose, set `DB_URL=jdbc:postgresql://app-db:5432/matriz`. A Compose `.env` file does not automatically populate an IDE or terminal process environment; configure those variables separately.

Flyway runs automatically at backend startup using the Spring Data JPA datasource. V1 creates teams, application profiles/provisioning state, projects, tasks (including the optimistic-lock version column used by JPA `@Version`), assignments and comments. The backend does not use Spring Modulith's persistent event-publication registry or dedicated audit storage. SQL script initialization is disabled. Flyway validates checksums, disables clean, and does not automatically baseline an existing unmanaged database. Add a new versioned migration for future changes; never edit an applied script. No seed data or Keycloak tables are included.

## Verification

From `matriz-backend`, run `mvnw.cmd -Dtest=ModuleStructureTests test` on Windows (or `./mvnw -Dtest=ModuleStructureTests test` elsewhere). The test checks the three detected modules and verifies Modulith boundaries without starting Spring or requiring PostgreSQL. Run `mvnw.cmd test` (or `mvn test` with installed Maven) with Docker running for the full suite. The Spring Boot integration tests use a disposable PostgreSQL 16.4 container, apply the same migration on startup, and cover repeat migration, checksum mismatch detection, constraints and cascades. They do not migrate the local Compose database.

The existing POM pins Spring Modulith 2.1.1, while `docs/architecture.md` records 2.1.2. Dependency alignment is outstanding; this scaffolding change preserves the existing dependency versions.

## Integrated HTTP edge

The customer requested one Java deployment unit (D-15). The `edge` infrastructure module now hosts the former gateway authentication controllers, CORS, JWT validation and Redis session revocation. Spring MVC remains the HTTP server; no Spring Cloud Gateway process or HTTP backend self-call is required. Administrator registration invokes `user.ProfileProvisioning` locally after Keycloak identity/role creation and compensates failures by deleting that new identity. Durable reconciliation remains outstanding.

The application listens on `SERVER_PORT` (default 8080). Configure `KEYCLOAK_URL`, `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_SET_URI`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, and the existing `KEYCLOAK_PROVISIONING_CLIENT_ID` / `KEYCLOAK_PROVISIONING_CLIENT_SECRET`. The provisioning service account must already be configured in Keycloak. `REDIS_HOST` and `REDIS_PORT` select the shared limiter/session store. `CORS_ALLOWED_ORIGIN_PATTERNS`, `CORS_ALLOWED_METHODS`, and `CORS_ALLOWED_HEADERS` retain the local defaults.

Every initial HTTP request, including public login and health endpoints, consumes the same five-token IP bucket, refilling five tokens per second. Limiter failure returns 429. `RATE_LIMIT_TRUSTED_PROXIES` is an optional comma-separated list of exact proxy IP addresses; all forwarded addresses from other peers are ignored. Keep `server.forward-headers-strategy=none`. Unknown/revoked sessions cannot access authenticated routes. Signed, unexpired tokens can retry logout after provider failure. Session-store failure denies access; limiter failure takes precedence with 429.

First run the backend verification gate to produce `target/matriz-backend-0.0.1-SNAPSHOT.jar`; the Docker image copies this verified artifact. Then build/run from the repository root with `docker compose --profile prod up --build`. Only the backend Java container is deployed. Existing frontend/observability services remain commented out. The legacy gateway folder is retained for reference. Do not run it in front of the consolidated backend.

Run `./mvnw verify` (Windows: `.\mvnw.cmd verify`) with Docker available for PostgreSQL integration tests. Focused checks: `.\mvnw.cmd -Dtest=LoginSecurityTests,IpRateLimitTests,ModuleStructureTests test`. Security tests use the servlet filter chain with simulated Keycloak/Redis; live identity, Redis crash durability, and back-channel logout require separate verification.

## Consolidation verification (2026-10-30)

- `.\mvnw.cmd clean verify`: 44 tests passed, zero failures/errors/skips; executable JAR produced. Includes PostgreSQL Flyway/constraints, local profile persistence and rejected-operation rollback, real Redis bucket sharing/refill, servlet authentication/logout/error handling and Modulith boundaries. A prior non-clean run found obsolete V2/V3 files in `target`; cleaning generated output resolved it without changing source migrations.
- `.\mvnw.cmd -Dtest=LoginSecurityTests test`: 17 passed after the final AAA test-layout adjustment.
- `docker compose --profile prod config --quiet`: valid configuration.
- An isolated Compose project built the image and ran backend, PostgreSQL, Redis and Keycloak. Live checks: health 200, anonymous protected access 401, invalid login 401, successful login 200, authenticated metrics 200, logout 204, access-token replay 401. A concurrent 12-request burst with different spoofed forwarding headers admitted five requests and rejected seven with 429. The sequential localhost probe was too slow to exhaust the bucket; the concurrent direct-loopback probe verified the intended burst.
- Temporary containers/networks/data volumes were removed. Existing working-tree changes and persistent application data were preserved. Diff whitespace validation passed.

Live registration still requires the pre-existing provisioning service-account setup; registration authorization/compensation was tested with simulated Keycloak, and profile persistence with real PostgreSQL. UI journeys, Keycloak back-channel logout, durable provisioning reconciliation, live multi-replica logout/crash recovery and telemetry delivery are not claimed as verified by this consolidation.


## Superseding decision D-16 (2026-10-30)

The customer removed backend session tracking and IP rate limiting. This supersedes all earlier Redis, limiter, immediate logout, back-channel revocation and immediate role-change invalidation requirements above. Redis is removed from the backend dependencies and Compose. Keycloak retains its own sessions and credential protections. The backend validates JWTs statelessly; logout terminates the Keycloak session, while issued access tokens and role claims remain usable until expiry (five minutes plus validator clock skew). Clients discard tokens on logout. Provider logout failure is reported and can be retried.

Valid tokens require no backend session registration. Missing, invalid and expired tokens remain rejected. Obsolete limiter tests were removed and security tests adjusted. Tests and Docker validation were not run, as requested; prior verification results apply only to the previous implementation. Legacy gateway source remains historical and is not deployed. Existing Redis volumes were not deleted.

## Dashboard endpoint

`GET /api/v1/dashboard` returns all accessible projects in name/ID order and task totals for `TODO`, `IN_PROGRESS` and `DONE` across the complete project dataset. Project Managers and Software Engineers receive project content; Administrators receive empty projects and zero task counts. The endpoint is authenticated and reads current database state on each request. Focused and module tests could not run in this environment because the Maven wrapper failed to start and the Spring Boot parent dependency was unavailable to installed Maven.
