# Backend scaffold

## Email-only identity

`POST /api/v1/auth/login` accepts `{"email":"user@example.com","password":"..."}`. Administrator-only `POST /api/v1/users` accepts `email`, `firstName`, `lastName`, `password`, `role` and `teamId`; no separate username is requested. Both endpoints reject missing, blank or malformed email with HTTP 400 before contacting Keycloak. Provisioning sets Keycloak's `username` and `email` to the same supplied email and keeps the local profile linked by the returned subject ID. The token endpoint's protocol parameter remains named `username` internally.

This replaces the former application `username` request field; update API callers alongside the backend/frontend. The imported realm enables **Email as username** (`registrationEmailAsUsername`) and **Login with email** (`loginWithEmailAllowed`), disables **Duplicate emails** (`duplicateEmailsAllowed`) and **Edit username** (`editUsernameAllowed`). The demo human accounts already have matching email/usernames. Keycloak-managed service accounts keep their existing usernames.

Startup import does not update an existing realm. In Keycloak administration, apply these realm settings and reconcile any existing human username that differs from its email, preserving user IDs, credentials, roles and application subject links. Resolve missing/duplicate emails before alignment. Do not delete users or volumes, or change application Flyway migrations to edit Keycloak data. Existing email login can work through `loginWithEmailAllowed` while usernames are reconciled. This change does not automatically mutate the running realm or existing accounts.

## Actuator access

All available Actuator web endpoints are exposed with `management.endpoints.web.exposure.include=*`. `/actuator` and `/actuator/**` permit anonymous requests for every HTTP method; individual endpoints retain their supported operations and availability settings. Approved on 2026-11-05; see the decision register.

The backend has three closed Spring Modulith modules under `com.wikicoding.matrizbackend`: `user`, `team`, and `projectmanagement`. Public types in each module root form its API. Nested packages are internal; use explicit module interfaces for cross-module access.

Each module contains documented packages for `domain`, `application`, `adapter.in.web`, `adapter.out.persistence`, and `configuration`. Project Management has separate domain packages for projects, tasks, assignments, and comments. These are package scaffolds, not implemented use cases or entities. Domain code must remain independent of Spring and persistence frameworks.

## PostgreSQL configuration

The backend uses Spring Data JPA. `application.properties` uses `DB_URL` for the complete PostgreSQL connection URL, defaulting to `jdbc:postgresql://localhost:5432/matriz`, and `APP_DB_USER` (default `matriz`). Set `APP_DB_PASSWORD` to the password configured for the Compose `app-db` service. No password is embedded in the backend configuration.

Standard Spring overrides `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` also work. For a backend running inside Compose, set `DB_URL=jdbc:postgresql://app-db:5432/matriz`. A Compose `.env` file does not automatically populate an IDE or terminal process environment; configure those variables separately.

Flyway runs automatically at backend startup using the Spring Data JPA datasource. V1 creates teams, application profiles/provisioning state, projects, tasks (including the optimistic-lock version column used by JPA `@Version`), assignments and comments. V4 seeds one local demo team and profiles for the three imported Keycloak demo users; their subjects match the fixed IDs in `config/keycloak/matriz-realm.json`. The backend does not use Spring Modulith's persistent event-publication registry or dedicated audit storage. SQL script initialization is disabled. Flyway validates checksums, disables clean, and does not automatically baseline an existing unmanaged database. Add a new versioned migration for future changes; never edit an applied script. No Keycloak tables are included.

The Keycloak realm import runs only for a new Keycloak data volume. An existing Keycloak volume retains its old user IDs, so V4 alone cannot link those users; reconcile their subjects with the actual Keycloak IDs before using the demo accounts. Do not delete volumes to perform that reconciliation.

## Bundled frontend

Install Node.js and npm on the Linux build machine and keep `matriz-frontend` beside `matriz-backend`. Maven runs `npm ci` and `npm run build` in that sibling directory during `generate-resources`, then copies the build into `target/classes/static` during `process-resources`. The Spring Boot JAR includes those files; Node.js is not needed in the runtime image.

Use `.\mvnw.cmd clean verify` on Windows (or `./mvnw clean verify` elsewhere) for a fresh deployment build, with Docker running for integration tests. Cleaning removes assets from previous frontend builds. For a focused build/copy check without Java tests, use `.\mvnw.cmd process-resources`. Maven lifecycle commands reaching this phase now also build the frontend and require npm registry access or a populated npm cache.

Leave `REACT_APP_API_BASE_URL` empty to use the backend's origin. Spring serves the bundled UI at `http://localhost:8080`; GET/HEAD access to the known UI routes and static assets is public. Known React page routes forward to `index.html` to support refresh/direct navigation. API authorization remains unchanged. Unmapped paths do not receive an SPA fallback.

## Viewing metrics and logs in Grafana

Open `http://<host>:3001`, replacing `<host>` with the Docker host (for local development, [localhost](http://localhost:3001)). Sign in using the configured Grafana credentials.

| Data | Navigation | Datasource | Filter |
| --- | --- | --- | --- |
| Backend metrics | **Drilldown > Metrics** | **Prometheus** | `job = matriz-backend` |
| Backend logs | **Drilldown > Logs** | **Loki** | `service_name = matriz-backend` |

Choose a time range covering recent backend activity. Prometheus scrapes metrics every 15 seconds; logs may take a few seconds to appear after a backend request. Historical console logs are not backfilled.

## Application logging

Logback writes to the console and to Loki using Spring Boot's OpenTelemetry SDK and OTLP HTTP exporter. The pinned OpenTelemetry Logback appender is installed with the Spring-managed SDK during startup. Only the `requestId` MDC attribute is captured. Export uses a bounded asynchronous queue (2048 records, batches up to 512, one-second interval) and a five-second HTTP timeout; telemetry delivery does not block request processing.

Appender 2.28.0-alpha targets the Boot 4.1.2 SDK baseline, 1.62.0. Keep instrumentation and SDK versions compatible: appender 2.21.0-alpha brings incubator API 1.55.0-alpha, which causes a startup `NoSuchMethodError` with SDK 1.62.0. After changing the pin, rebuild the backend image. Check the resolved graph with `.\mvnw.cmd dependency:tree '-Dincludes=io.opentelemetry:*,io.opentelemetry.instrumentation:*'`; the incubator API should resolve to 1.62.0-alpha alongside SDK/API 1.62.0.

Compose sets `MANAGEMENT_OPENTELEMETRY_LOGGING_EXPORT_OTLP_ENDPOINT=http://loki:3100/otlp/v1/logs`. For an IDE process, the default is `http://localhost:3100/otlp/v1/logs`; override with `OTLP_LOGS_ENDPOINT`. Loki accepts structured metadata with the existing v13 schema. This exports backend logs, not other containers' console output. Logs produced before the appender is installed may be absent.

Apply logging changes from the repository root with `docker compose --profile prod up -d --build backend loki grafana`. See [Viewing metrics and logs in Grafana](#viewing-metrics-and-logs-in-grafana) for navigation and filters. Run the focused delivery and boundary checks from this directory with `.\mvnw.cmd '-Dtest=OpenTelemetryLoggingTests,ModuleStructureTests' test`.

Application logs default to INFO. Set `APP_LOG_LEVEL=DEBUG` in the backend process environment to enable API request summaries, login/logout outcomes, account-provisioning compensation and Keycloak call status/timing. Routine requests and expected client errors do not emit INFO/WARN entries. HTTP 5xx responses, unhandled request failures and failed provisioning compensation emit ERROR entries.

Team, project, task, comment and assignment services also use DEBUG for operation progress, fixed rejection reasons, task status/priority, eligibility checks and assignment cleanup after project-team changes or task moves. Write/flush entries explicitly await transaction commit; they are not proof of committed success. Unexpected failures remain ERROR at the HTTP boundary to avoid duplicate error logs. No business content or identity/resource identifiers are logged. The console includes the request ID through MDC while executing the servlet dispatch, linking synchronous service decisions to the HTTP summary; this context is restored on exit and is not automatically propagated to reactive worker threads. Enable just `logging.level.com.wikicoding.matrizbackend.team.services=DEBUG` or `logging.level.com.wikicoding.matrizbackend.projectmanagement.services=DEBUG` for focused module diagnostics.

API summaries include a generated request ID, the matched route template, status and elapsed milliseconds. MVC asynchronous responses are logged on their completing dispatch. Unmatched requests (including rejections before MVC) use `unmapped`; static assets and Actuator polling are excluded. Raw URLs, query strings, client addresses, identity values, headers and response bodies are not logged. Exception diagnostics report the exception class only, because exception messages and causes may contain confidential values.

To inspect sanitized request bodies, also set `APP_LOG_REQUEST_BODIES=true`. Both this flag and DEBUG are required. This logs the converted request DTO with field names; only typed task status, priority and effort values are retained. All other values, including names, descriptions, comments, identifiers, passwords and tokens, are redacted by default. No raw-body fallback is used: malformed JSON and requests rejected before conversion have no body entry. Unknown/non-record body types have an empty field projection. Body logging does not buffer or consume the request stream separately. Keep framework HTTP, security and SQL payload logging disabled when handling confidential data.

For narrower diagnostics, keep `APP_LOG_LEVEL=INFO` and configure `logging.level.com.wikicoding.matrizbackend.apigateway.configs.HttpRequestLoggingFilter=DEBUG` and/or `logging.level.com.wikicoding.matrizbackend.apigateway.configs.RequestBodyLoggingAdvice=DEBUG` in the backend configuration. These settings use existing SLF4J/Logback support and add no telemetry exporter or retention policy.

## Verification commands

`FrontendServingTests` uses dedicated resources from `src/test/resources/frontend-fixtures` to verify the backend's page forwarding, static-resource access and security rules. These checks do not require a frontend build and do not verify frontend packaging; the Maven frontend build/copy steps were removed when the Docker services were separated.

From `matriz-backend`, run `mvnw.cmd -Dtest=ModuleStructureTests test` on Windows (or `./mvnw -Dtest=ModuleStructureTests test` elsewhere). The test checks the three detected modules and verifies Modulith boundaries without starting Spring or requiring PostgreSQL. Run `mvnw.cmd test` (or `mvn test` with installed Maven) with Docker running for the full suite. The Spring Boot integration tests use a disposable PostgreSQL 16.4 container, apply the same migration on startup, and cover repeat migration, checksum mismatch detection, constraints and cascades. They do not migrate the local Compose database.

The existing POM pins Spring Modulith 2.1.1, while `docs/architecture.md` records 2.1.2. Dependency alignment is outstanding; this scaffolding change preserves the existing dependency versions.

## Integrated HTTP edge

The customer requested one Java deployment unit (D-15). The `edge` infrastructure module now hosts the former gateway authentication controllers, CORS, JWT validation and Redis session revocation. Spring MVC remains the HTTP server; no Spring Cloud Gateway process or HTTP backend self-call is required. Administrator account creation at `POST /api/v1/users` invokes the User module after Keycloak identity/role creation and compensates a reported local failure by deleting that new identity. Provisioning retry/reconciliation after interrupted requests is still required by the requirements and is not implemented here.

The application listens on `SERVER_PORT` (default 8080). Configure `KEYCLOAK_URL`, `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_SET_URI`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, and the existing `KEYCLOAK_PROVISIONING_CLIENT_ID` / `KEYCLOAK_PROVISIONING_CLIENT_SECRET`. The provisioning service account must already be configured in Keycloak. `REDIS_HOST` and `REDIS_PORT` select the shared limiter/session store. `CORS_ALLOWED_ORIGIN_PATTERNS`, `CORS_ALLOWED_METHODS`, and `CORS_ALLOWED_HEADERS` retain the local defaults.

Every initial HTTP request, including public login and health endpoints, consumes the same five-token IP bucket, refilling five tokens per second. Limiter failure returns 429. `RATE_LIMIT_TRUSTED_PROXIES` is an optional comma-separated list of exact proxy IP addresses; all forwarded addresses from other peers are ignored. Keep `server.forward-headers-strategy=none`. Unknown/revoked sessions cannot access authenticated routes. Signed, unexpired tokens can retry logout after provider failure. Session-store failure denies access; limiter failure takes precedence with 429.

Build and run from the repository root with `docker compose --profile prod up --build`. The Docker build packages the frontend and backend in a build stage, then copies the JAR into the runtime image. The image build skips Java tests; run the verification gate separately with Docker available for integration tests. Only the backend Java container is deployed. Existing frontend/observability services remain commented out. The legacy gateway folder is retained for reference. Do not run it in front of the consolidated backend.

Run `./mvnw verify` (Windows: `.\mvnw.cmd verify`) with Docker available for PostgreSQL integration tests. Focused checks: `.\mvnw.cmd -Dtest=LoginSecurityTests,IpRateLimitTests,ModuleStructureTests test`. Security tests use the servlet filter chain with simulated Keycloak/Redis; live identity, Redis crash durability, and back-channel logout require separate verification.

## Consolidation verification (2026-10-30)

- `.\mvnw.cmd clean verify`: 44 tests passed, zero failures/errors/skips; executable JAR produced. Includes PostgreSQL Flyway/constraints, local profile persistence and rejected-operation rollback, real Redis bucket sharing/refill, servlet authentication/logout/error handling and Modulith boundaries. A prior non-clean run found obsolete V2/V3 files in `target`; cleaning generated output resolved it without changing source migrations.
- `.\mvnw.cmd -Dtest=LoginSecurityTests test`: 17 passed after the final AAA test-layout adjustment.
- `docker compose --profile prod config --quiet`: valid configuration.
- An isolated Compose project built the image and ran backend, PostgreSQL, Redis and Keycloak. Live checks: health 200, anonymous protected access 401, invalid login 401, successful login 200, authenticated metrics 200, logout 204, access-token replay 401. A concurrent 12-request burst with different spoofed forwarding headers admitted five requests and rejected seven with 429. The sequential localhost probe was too slow to exhaust the bucket; the concurrent direct-loopback probe verified the intended burst.
- Temporary containers/networks/data volumes were removed. Existing working-tree changes and persistent application data were preserved. Diff whitespace validation passed.

The historical live registration check required provisioning service-account setup; The historical registration authorization/compensation check used simulated Keycloak, and profile persistence used real PostgreSQL. UI journeys, Keycloak back-channel logout, durable provisioning reconciliation, live multi-replica logout/crash recovery and telemetry delivery are not claimed as verified by this consolidation.


## Superseding decision D-16 (2026-10-30)

The customer removed backend session tracking and IP rate limiting. This supersedes all earlier Redis, limiter, immediate logout, back-channel revocation and immediate role-change invalidation requirements above. Redis is removed from the backend dependencies and Compose. Keycloak retains its own sessions and credential protections. The backend validates JWTs statelessly; logout terminates the Keycloak session, while issued access tokens and role claims remain usable until expiry (five minutes plus validator clock skew). Clients discard tokens on logout. Provider logout failure is reported and can be retried.

Valid tokens require no backend session registration. Missing, invalid and expired tokens remain rejected. Obsolete limiter tests were removed and security tests adjusted. Tests and Docker validation were not run, as requested; prior verification results apply only to the previous implementation. Legacy gateway source remains historical and is not deployed. Existing Redis volumes were not deleted.

## Dashboard endpoint

`GET /api/v1/dashboard` returns all accessible projects in name/ID order and task totals for `TODO`, `IN_PROGRESS` and `DONE` across the complete project dataset. Project Managers and Software Engineers receive project content; Administrators receive empty projects and zero task counts. The endpoint is authenticated and reads current database state on each request. Focused and module tests could not run in this environment because the Maven wrapper failed to start and the Spring Boot parent dependency was unavailable to installed Maven.

## User/team listings and task assignments

`GET /api/v1/users` and `GET /api/v1/teams` accept zero-based `page` and `size` (default 0 and 20, maximum 100) and return page metadata. Administrator user listings contain completed, active local profiles ordered by email and ID; team listings order by name and ID. Task assignment GET/POST/DELETE routes expose zero or one assignee. POST checks the project team and the current Software Engineer role in Keycloak; the task ID uniqueness constraint protects concurrent assignment. Project Managers mutate assignments and Software Engineers may read them. These paths require focused tests and live identity/database verification before acceptance.

The panel can read the current profile at `GET /api/v1/users/me`. Project Managers and Software Engineers can resolve owner, assignee and comment-author names through `GET /api/v1/users/{userId}/display`, and can read team names through `GET /api/v1/teams/{teamId}`. Team listing is also readable by Project Managers and Software Engineers for the project form; only Administrators create teams. Project Managers can populate the single-assignee selector with `GET /api/v1/projects/{projectId}/assignee-options`, which includes active local members of the project team who currently have the Software Engineer realm role. Assignment creation rechecks eligibility.


## In-memory IP rate limiting (2026-11-13)

The customer reinstated backend IP rate limiting, superseding the limiter removal in D-16 and the older Redis/token-bucket policy. Every incoming backend HTTP request shares a maximum of five admissions per rolling one-second window per client IP, including authentication, Actuator, OPTIONS and unmapped routes. Rejected requests return HTTP 429 with the existing code/message JSON envelope and `Retry-After: 1`; they do not extend the window. Async/error redispatches are not counted again. Authentication failures consume an admission.

State is in memory in each backend process and resets on restart; it is not shared across replicas. Shared NAT addresses share a quota. No Redis or additional dependency is required. Direct frontend and Keycloak endpoints are outside the backend filter's scope.

`RATE_LIMIT_TRUSTED_PROXIES` defaults to empty and accepts comma-separated exact IP literals. Forwarded headers from other socket peers are ignored. For trusted peers, walk X-Forwarded-For from right to left to the first untrusted address; malformed chains fall back to the peer. Keep server forwarding disabled. The frontend proxy appends forwarding information; configure its actual backend-facing IP as trusted to preserve distinct client quotas. Do not trust arbitrary client networks.

`RATE_LIMIT_MAX_IPS` defaults to 10000. Cleanup occurs lazily on traffic, at most once per second. Capacity exhaustion rejects new addresses with 429 rather than evicting active quotas. Existing addresses continue under their quotas. Storage remains bounded during idle periods.

The API contract is `openapi.yaml`. Focused limiter/filter tests were added, but tests and application startup were deliberately not run at the customer's request. Full runtime validation remains pending.
