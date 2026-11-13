# Matriz

Matriz is an English-language application for approximately 500 Project Managers and Software Engineers to manage projects and tasks.

## Revised MVP

The customer adopted Practical Assignment 03 on 2026-10-29. The MVP includes login/logout and profile, accessible project and task management with deletion, zero or one Software Engineer assignee, comments, priority, paginated/sorted listings, task filters and a home panel with task counts. Administrators create and view users and teams, assigning one role and one team when creating each user; backend authorization combines roles with project access. Remaining policy and production acceptance details are listed in the decision register.

User/team updates and deletion were not requested and are outside scope, including post-creation role and membership changes. Comments support creation/reading only and are removed only by project/task deletion cascades. See D-17 in [constraints](docs/constraints.md). This documentation/API revision does not establish implementation alignment.

## Selected architecture

Java 21, Spring Boot/Spring Modulith, Clean Architecture, an integrated HTTP security/authentication edge in the same backend process, IP rate limiting, Keycloak and PostgreSQL with Flyway migrations; React with React Router and reusable UI components. Local Docker Compose runs the backend, Keycloak, PostgreSQL and Redis. Grafana/Loki and Prometheus configuration is present, but their Compose services remain commented out. Production hosting is deferred; current delivery targets local Docker Compose. The confirmed SLA is 99.95%.

Testing choices: Mockito for backend units; Spring Boot with Testcontainers for integration; Jest for UI units and Playwright for browser journeys.

The backend has User, Team and Project Management modules plus HTTP edge infrastructure in one deployable process. Project Management groups projects, tasks, assignments and comments with distinct internal responsibilities, reflecting their shared lifecycle and business rules.

All backend application services and modules must be deployed together as one deployment unit; independent backend service or module deployment is not permitted. This is a financial decision, not a technical decision. See the confirmed backend deployment constraint in [constraints](docs/constraints.md).

## Documentation

- [Requirements](docs/requirements.md) — revised product scope and rules.
- [User stories](docs/user-stories.md) — acceptance criteria and dependencies.
- [Constraints](docs/constraints.md) — confirmed choices and authoritative pending decisions.
- [Architecture](docs/architecture.md) — stack, module boundaries and verification strategy.
- [Data model](docs/database.md) — revised entities and relationships.
- [Wireframe](docs/ui/wireframe.png) — revised MVP screen overview; [design notes](docs/ui/wireframe.md).
- [API contract](openapi.yaml) — revised endpoint and schema draft; role permissions and operational limits reflect approved decisions.

## Running locally

For the requested AWS host preparation, see the [EC2 CloudFormation template and
instructions](infra/cloudformation/README.md). This provisions host software only;
application deployment and production acceptance remain separate.

The frontend and backend are separate containers. Maven no longer bundles the React UI in the Spring Boot JAR. The current frontend Dockerfile runs `npm start` on port 3000 and proxies API requests to the backend.

### Publishing application images

After both CI jobs succeed on a push to `main`, `.github/workflows/cicd.yml` ensures the required separate `linux/amd64` images are published from the existing Dockerfiles:

- `ghcr.io/<owner>/<repository>-backend:<component-commit-sha>`
- `ghcr.io/<owner>/<repository>-frontend:<component-commit-sha>`

Image names are lowercased. Publishing uses the workflow's `GITHUB_TOKEN` with `packages: write`; no additional registry secret is required. Pull requests run checks without publishing. Each image uses the last commit affecting its component or shared build inputs; unchanged published images are reused. Deployment waits for the complete publish matrix. Existing packages must grant this repository Actions access if not already linked. After publishing succeeds, the workflow connects to EC2 as `ubuntu` and invokes the installed deployment script as root through non-interactive sudo. Configure the `ec2` environment and prepare the host as described in [continuous deployment](docs/deployment.md). Supporting Keycloak/observability images are not republished or redeployed.

Docker Compose keeps the application services behind the `prod` profile. Supporting services can be started independently while developing infrastructure or application components.

Start the supporting services:

```text
docker compose up
```

Build and start the backend and supporting services. The image build packages the backend and frontend:

```text
docker compose --profile prod up
```

Run the complete application in the background:

```text
docker compose --profile prod up -d
```

Rebuild application images before starting:

```text
docker compose --profile prod up --build
```

Stop the running services while preserving persistent volumes:

```text
docker compose --profile prod down
```

On first startup with an empty Keycloak database, the Compose setup imports the `matriz` realm, the `matriz-client` browser client, the `matriz-provisioning` service client, and one temporary-password user for each application role. The provisioning client uses `KEYCLOAK_PROVISIONING_CLIENT_SECRET`, which defaults only for local development; set this environment variable to a different value before starting Compose if required. The realm import does not update an existing realm; add missing clients through the Keycloak Admin Console.
The backend migration V4 also creates a demo team and local profiles linked to these imported users on fresh Keycloak databases. Existing Keycloak databases retain their original user IDs and require manual subject reconciliation for these profiles before frontend login can complete.

| Role | Email | Initial password |
| --- | --- | --- |
| Administrator | `admin@matriz.local` | `MatrizAdmin123!` |
| Project Manager | `manager@matriz.local` | `MatrizManager123!` |
| Software Engineer | `engineer@matriz.local` | `MatrizEngineer123!` |

These initial credentials are for local development only and must not be reused elsewhere.

Login requests use `email` and `password`. User creation also accepts only email as the account identifier; the backend sets the Keycloak username equal to email. See [email identity setup](matriz-backend/README.md#email-only-identity) for existing realms.

Accounts created through `POST /api/v1/users` are marked email verified and can log in directly with the supplied password. This does not retroactively clear `UPDATE_PASSWORD` for accounts created before the 2026-10-31 policy change.

Observability services are enabled in Compose. Prometheus scrapes the backend at `/actuator/prometheus`; backend Logback logs are exported asynchronously over OTLP HTTP to Loki. Grafana provisions both datasources, with Prometheus as the default. Open `http://<host>:3001` (`http://localhost:3001` locally): use **Drilldown > Metrics** with `job = matriz-backend` for metrics, or **Drilldown > Logs** with `service_name = matriz-backend` for logs. See [Grafana navigation](matriz-backend/README.md#viewing-metrics-and-logs-in-grafana). Rebuild/apply logging changes with `docker compose --profile prod up -d --build backend loki grafana`. See [logging setup](matriz-backend/README.md#application-logging) for endpoint overrides and focused checks. Logs from other containers and historical console output are not collected by this pipeline.

## Status

Schema design and API contracts are approved with the customer revisions recorded in constraints.md, except that the API baseline must be revised for the subsequently approved pagination/list-sorting requirement. The text documentation reflects single optional assignment, UUID-only resource identifiers, physical deletion, server-local timestamps, Redis limiting, and paginated/sorted listings. The PNG wireframe is an older illustration. Core business rules and technical completion are approved/delegated. Business audit storage and a fixed operational-log retention period are not required; response time must be under two seconds at p95. Production is deferred. Local implementation can start once the API pagination contract is revised; load-test conditions and future production SLA measurement remain acceptance details in the decision register. The backend now has User, Team and Project Management package scaffolds, environment-based PostgreSQL connection settings and a module-boundary test; see [backend setup](matriz-backend/README.md). The Flyway migration initializes only application and provisioning tables; no unused Modulith event-publication or audit tables are created. Business behavior remains unimplemented.

## Single Java deployment (2026-10-30)

Run `docker compose --profile prod up --build` from the repository root to build the consolidated backend at `http://localhost:8080`. Login, registration, logout, IP limiting and session validation now run in this process. The separate `matriz-api-gateway` is not deployed. Supporting Keycloak, PostgreSQL and Redis services remain separate. Frontend and observability services are currently commented out in Compose. See [backend setup](matriz-backend/README.md) for configuration and verification limits.


## Superseding decision D-16 (2026-10-30)

The customer removed backend session tracking and IP rate limiting. This supersedes all earlier Redis, limiter, immediate logout, back-channel revocation and immediate role-change invalidation requirements above. Redis is removed from the backend dependencies and Compose. Keycloak retains its own sessions and credential protections. The backend validates JWTs statelessly; logout terminates the Keycloak session, while issued access tokens and role claims remain usable until expiry (five minutes plus validator clock skew). Clients discard tokens on logout. Provider logout failure is reported and can be retried.

Valid tokens require no backend session registration. Missing, invalid and expired tokens remain rejected. Obsolete limiter tests were removed and security tests adjusted. Tests and Docker validation were not run, as requested; prior verification results apply only to the previous implementation. Legacy gateway source remains historical and is not deployed. Existing Redis volumes were not deleted.

## Keycloak PostgreSQL storage

Keycloak uses its own `keycloak` database and login on the existing `app-db` PostgreSQL service. Keycloak creates and migrates its tables; application Flyway migrations remain separate. Both databases persist in `app-db-data`.

Compose runs the one-shot `keycloak-db-init` service after PostgreSQL is healthy and before starting Keycloak. It creates the role/database only when missing, including with an existing PostgreSQL volume. It does not reset existing passwords or data. The PostgreSQL bootstrap user must retain permission to create roles and databases.

Configure `KEYCLOAK_DB_NAME`, `KEYCLOAK_DB_USER` and `KEYCLOAK_DB_PASSWORD` before startup if overriding the local defaults. Database and role names must differ from the application's. For an existing role, supply its actual password; changing the environment does not rotate it. The initializer connects with `APP_DB_USER` and `APP_DB_PASSWORD`; set these to the credentials already stored in your PostgreSQL volume. Set `APP_DB_PASSWORD` explicitly because the existing backend and PostgreSQL local fallback values differ.

Apply with `docker compose --profile prod up -d`. This switches Keycloak from its embedded development database to PostgreSQL and imports the configured realm into an empty database. Existing embedded identities, sessions and configuration are not copied. Export any required realm/users before recreating the old Keycloak container; existing application profiles may need subject reconciliation. Do not delete PostgreSQL volumes to apply this change.


## In-memory IP rate limiting (2026-11-13)

The customer reinstated backend IP rate limiting, superseding the limiter removal in D-16 and the older Redis/token-bucket policy. Every incoming backend HTTP request shares a maximum of five admissions per rolling one-second window per client IP, including authentication, Actuator, OPTIONS and unmapped routes. Rejected requests return HTTP 429 with the existing code/message JSON envelope and `Retry-After: 1`; they do not extend the window. Async/error redispatches are not counted again. Authentication failures consume an admission.

State is in memory in each backend process and resets on restart; it is not shared across replicas. Shared NAT addresses share a quota. No Redis or additional dependency is required. Direct frontend and Keycloak endpoints are outside the backend filter's scope.

`RATE_LIMIT_TRUSTED_PROXIES` defaults to empty and accepts comma-separated exact IP literals. Forwarded headers from other socket peers are ignored. For trusted peers, walk X-Forwarded-For from right to left to the first untrusted address; malformed chains fall back to the peer. Keep server forwarding disabled. The frontend proxy appends forwarding information; configure its actual backend-facing IP as trusted to preserve distinct client quotas. Do not trust arbitrary client networks.

`RATE_LIMIT_MAX_IPS` defaults to 10000. Cleanup occurs lazily on traffic, at most once per second. Capacity exhaustion rejects new addresses with 429 rather than evicting active quotas. Existing addresses continue under their quotas. Storage remains bounded during idle periods.

The API contract is `openapi.yaml`. Focused limiter/filter tests were added, but tests and application startup were deliberately not run at the customer's request. Full runtime validation remains pending.
