# Matriz

Matriz is an English-language application for approximately 500 Project Managers and Software Engineers to manage projects and tasks.

## Revised MVP

The customer adopted Practical Assignment 03 on 2026-10-29. The MVP includes login/logout and profile, accessible project and task management with deletion, multiple assignees, comments, priority, project-name search, task filters and a home panel with task counts. Administrators manage users/team membership; backend authorization combines roles with project access. Unspecified access and lifecycle rules remain in the decision register.

## Selected architecture

Java 21, Spring Boot/Spring Modulith, Clean Architecture, a separate reactive Spring Cloud Gateway, IP rate limiting, Keycloak and PostgreSQL; React with React Router and reusable UI components. Local Docker Compose runs the complete environment. Hosting is undecided (AWS is a candidate). The confirmed SLA is 99.95%.

Testing choices: Mockito for backend units; Spring Boot with Testcontainers for integration; Jest for UI units and Playwright for browser journeys.

## Documentation

- [Requirements](docs/requirements.md) — revised product scope and rules.
- [User stories](docs/user-stories.md) — acceptance criteria and dependencies.
- [Constraints](docs/constraints.md) — confirmed choices and authoritative pending decisions.
- [Architecture](docs/architecture.md) — stack, module boundaries and verification strategy.
- [Data model](docs/database.md) — revised entities and relationships.
- [Wireframe](docs/ui/wireframe.png) — revised MVP screen overview; [design notes](docs/ui/wireframe.md).
- [API contract](openapi.yaml) — earlier draft, awaiting reconciliation with the revised requirements.

## Status

The modeling/architecture documentation and UI wireframe reflect the revised MVP. The API contract still reflects the earlier scope and must be reconciled before implementation. Detailed schema, access, lifecycle and operational decisions remain open; see constraints.md. No application code, setup/test commands, migrations or deployment configuration exist yet. Implementation planning and verification evidence will be added when that work is authorized.
