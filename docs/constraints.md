# Constraints and pending decisions

## Confirmed decisions

The customer approved the expanded scope of Practical Assignment 03 on 2026-10-29. It supersedes conflicting initial requirements: multiple assignees replace one assignee, deletion replaces project archival as the required workflow, and project access is scoped rather than globally available. Example field rules adopted for this revision are stated in requirements.md.

- English application and documentation; approximately 500 users.
- Java 21; Spring Boot with the latest stable Spring Modulith (2.1.2 verified on 2026-10-29); modular monolith with Clean Architecture inside backend modules.
- Separate Spring Cloud Gateway Server WebFlux gateway; rate limiting on every endpoint by client IP.
- Keycloak owns authentication, credentials, and roles; PostgreSQL stores application data.
- Docker Compose runs the complete local environment. Deployment provider is undecided; AWS is a candidate, not a requirement.
- Confirmed availability SLA: **99.95%**. Measurement details remain pending.
- Mockito for backend unit tests. Selected integration approach: Spring Boot tests with Docker-backed Testcontainers.
- React, React Router, local state and optional useContext; no Flux, useReducer, or Redux Toolkit. Reusable UI elements must be shared components. Custom CSS selected; Tailwind CSS remains an allowed alternative.
- Jest selected for UI unit tests; Playwright for browser integration/end-to-end tests.
- Spring validation and global backend exception handlers are allowed and included in the architecture.
- This change is documentation only. No application, infrastructure, migrations, or API contract implementation is authorized in this task.

## Decision register

This is the authoritative pending-decision register. Confirmed choices above are not reopened by the remaining items. Proposed details below are not approval to implement them; block only affected work.

| ID   | Remaining decision                                                                                                                                                                                                             | Blocks                                                 |
|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| D-01 | Approve detailed module interfaces, physical schema and revised public API contracts. Stack and modular-monolith style are confirmed.                                                                                          | Affected contracts/schema implementation.              |
| D-02 | Define Keycloak account provisioning, application-role mapping, role-change permissions, identity/profile synchronization and email uniqueness. Keycloak is the role authority.                                                | Provisioning and detailed identity integration.        |
| D-03 | Decide user-to-team cardinality and team lifecycle administration.                                                                                                                                                             | Membership and team administration.                    |
| D-04 | Decide project/task identifier generation and format.                                                                                                                                                                          | Identifier creation and validation.                    |
| D-05 | Define eligible assignees, whether they must be Software Engineers/on the project team, minimum assignee count, and behavior after membership changes.                                                                         | Assignment eligibility and updates.                    |
| D-06 | Define logical versus physical deletion, dependent task/comment/assignment handling, retention and treatment of any archived records. Deletion is now required; archival is no longer its substitute.                          | Deletion persistence and lifecycle behavior.           |
| D-07 | Define timestamp format and timezone.                                                                                                                                                                                          | Storage and API/display timestamp contracts.           |
| D-08 | Define SLA measurement window, monitored scope, exclusions, and performance targets. The 99.95% value is confirmed.                                                                                                            | Availability acceptance and capacity design.           |
| D-09 | Select deployment provider (AWS is optional), hosting, networking, backup/recovery and monitoring.                                                                                                                             | Production deployment.                                 |
| D-10 | Define password/recovery/session/lockout, privacy, audit and regulatory policies.                                                                                                                                              | Related security behavior and release readiness.       |
| D-11 | Define project sharing/access beyond its owner, administrator visibility, comment permissions and assignment-management permissions. Retain existing roles without assuming global project access.                             | Detailed authorization matrix and collaboration flows. |
| D-12 | Define IP rate/burst quotas, global versus per-route buckets, trusted proxy handling, distributed limiter/store, outage policy and coverage of identity/operational endpoints. Redis is an option, not an approved dependency. | Limiter implementation and deployment topology.        |

## Scope

The revised MVP includes authentication/session profile, project/task management and deletion, multiple assignees, comments, project-name search, task status/priority filters and a home panel with task counts. Backlogs, Kanban, general reports/analytics and external business integrations remain outside scope. Public registration and password recovery are optional assignment extensions, not approved MVP features.

The unchanged openapi.yaml predates this revision and must be reconciled before implementation. The [wireframe](ui/wireframe.png) now illustrates the revised scope; its illustrative controls do not resolve pending permissions or lifecycle policies.
