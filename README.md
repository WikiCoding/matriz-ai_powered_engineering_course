# Matriz

Matriz is an English-language application for approximately 500 Project Managers and Software Engineers to manage projects and tasks.

## Revised MVP

The customer adopted Practical Assignment 03 on 2026-10-29. The MVP includes login/logout and profile, accessible project and task management with deletion, zero or one Software Engineer assignee, comments, priority, project-name search, task filters and a home panel with task counts. Administrators manage users/team membership; backend authorization combines roles with project access. Remaining policy and production acceptance details are listed in the decision register.

## Selected architecture

Java 21, Spring Boot/Spring Modulith, Clean Architecture, a separate reactive Spring Cloud Gateway, IP rate limiting, Keycloak and PostgreSQL with Flyway migrations; React with React Router and reusable UI components. Local Docker Compose runs the complete environment with Grafana/Loki, Prometheus metrics storage, and Actuator/Micrometer OTLP export. Production hosting is deferred; current delivery targets local Docker Compose. The confirmed SLA is 99.95%.

Testing choices: Mockito for backend units; Spring Boot with Testcontainers for integration; Jest for UI units and Playwright for browser journeys.

The backend has User, Team and Project Management modules in one deployable process. Project Management groups projects, tasks, assignments and comments with distinct internal responsibilities, reflecting their shared lifecycle and business rules.

## Documentation

- [Requirements](docs/requirements.md) — revised product scope and rules.
- [User stories](docs/user-stories.md) — acceptance criteria and dependencies.
- [Constraints](docs/constraints.md) — confirmed choices and authoritative pending decisions.
- [Architecture](docs/architecture.md) — stack, module boundaries and verification strategy.
- [Data model](docs/database.md) — revised entities and relationships.
- [Wireframe](docs/ui/wireframe.png) — revised MVP screen overview; [design notes](docs/ui/wireframe.md).
- [API contract](openapi.yaml) — revised endpoint and schema draft; role permissions and operational limits reflect approved decisions.

## Running locally

Docker Compose keeps the application services behind the `prod` profile. Supporting services can be started independently while developing infrastructure or application components.

Start the supporting services:

```text
docker compose up
```

Start the complete local application, including the web client, gateway and backend:

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

On first startup with a new Keycloak volume, the Compose setup imports the `matriz` realm, the `matriz-client` browser client, and one temporary-password user for each application role:

| Role | Username | Initial password |
| --- | --- | --- |
| Administrator | `admin@matriz.local` | `MatrizAdmin123!` |
| Project Manager | `manager@matriz.local` | `MatrizManager123!` |
| Software Engineer | `engineer@matriz.local` | `MatrizEngineer123!` |

The initial passwords require changing on first login. These credentials are for local development only and must not be reused elsewhere.

Observability is preconfigured for Spring Boot 4.x Actuator/Micrometer applications. Prometheus scrapes the backend and gateway at `/actuator/prometheus`, and Grafana automatically provisions Prometheus and Loki datasources. Open Grafana at `http://localhost:3001` using the configured local Grafana credentials.

## Status

Schema design and API contracts are approved with the customer revisions recorded in constraints.md. The text documentation and API reflect single optional assignment, UUID-only resource identifiers, physical deletion, server-local timestamps and Redis limiting. The PNG wireframe is an older illustration. Core business rules and technical completion are approved/delegated. Audit and operational logs are retained for one year; response time must be under two seconds at p95. Production is deferred. Local implementation can start; load-test conditions and future production SLA measurement remain acceptance details in the decision register. No application code or test/setup commands exist yet.
