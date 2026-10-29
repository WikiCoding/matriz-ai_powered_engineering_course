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

## Status

Schema design and API contracts are approved with the customer revisions recorded in constraints.md. The text documentation and API reflect single optional assignment, UUID-only resource identifiers, physical deletion, server-local timestamps and Redis limiting. The PNG wireframe is an older illustration. Core business rules and technical completion are approved/delegated. Audit and operational logs are retained for one year; response time must be under two seconds at p95. Production is deferred. Local implementation can start; load-test conditions and future production SLA measurement remain acceptance details in the decision register. No application code or test/setup commands exist yet.
