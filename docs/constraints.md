# Constraints and pending decisions

## Confirmed decisions

Customer decisions on 2026-10-29 supersede conflicting earlier drafts.

- English application/documentation; approximately 500 users.
- Java 21, Spring Boot/Spring Modulith modular monolith and Clean Architecture; separate Spring Cloud Gateway Server WebFlux.
- Keycloak owns credentials and roles; PostgreSQL owns application data.
- Docker Compose runs the full local environment, including Redis, Grafana, Loki and the selected Prometheus metrics store.
- React, React Router, local state and optional useContext; no Flux, useReducer or Redux Toolkit. Custom CSS remains selected; Tailwind CSS and shadcn/ui are permitted alternatives. shadcn/ui may use Tailwind styling; permission to use it does not prohibit that dependency.
- Mockito, Spring Boot/Testcontainers, Jest and Playwright are selected.
- Availability target is 99.95%; production hosting is deferred. Response-time target is p95 under two seconds. Retain audit records and operational logs for one year; no organizational policy is currently defined.
- D-01: existing schema design and API contracts are approved, subject to the explicit revisions below. The repository contains a logical model, not a detailed physical DDL specification. Completion of detailed module interfaces is now delegated.
- D-02: Administrators create accounts and communicate temporary passwords in person; password change is mandatory at first login. Keycloak role mapping is in requirements.md. Sessions have an absolute maximum of eight hours, then require login again; access tokens need not last eight hours.
- D-03: each user belongs to exactly one team; Administrators manage team lifecycle and membership.
- D-04 (resolved): UUID resource IDs only for projects/tasks; no separate incremental identifiers or visible identifier fields.
- D-05: zero or one assignee, who must be a Software Engineer in the project's team. Moving a user to another team automatically removes assignments in their old team as part of the move.
- D-06 (resolved): physical cascade deletion. Project deletion removes its tasks and their comments/assignments; task deletion removes its comments/assignment. Preserve users and teams; leave no orphans.
- D-07 (resolved): server timezone with 24-hour `dd/MM/yyyy HH:mm:ss`. Configure application instances consistently.
- D-11: only Software Engineers create comments; Project Managers and Software Engineers read comments; Project Managers manage assignments. All Project Managers and Software Engineers can access all projects, regardless of ownership or team. Resource/role checks still apply.
- D-12: one Redis token bucket per client IP shared across routes and instances; refill five tokens/second, capacity five, one token per request. This is an average rate with a small burst, not a strict rolling-window count. Trust forwarded IP headers only from explicitly configured proxies; ignore them otherwise. Quota exhaustion or limiter outage returns HTTP 429.
- D-13 (resolved, delegated selection): Flyway with versioned PostgreSQL SQL migrations; see architecture.md.
- This update authorizes documentation and contract revisions; no application implementation is included.

## Remaining decision register

Approvals of proposals 1–8 settle business behavior and delegate technical security/schema/interface completion. Do not reopen delegated implementation details as customer blockers. Production is now explicitly deferred, retention is one year and the response-time percentile is p95. No unresolved business-rule decision blocks starting local implementation.

| ID | Remaining decision | Blocks |
| --- | --- | --- |
| D-08 | p95 response time must be under two seconds. Measurement boundary, concurrent load and dataset size still need a documented test scenario. SLA window, scope and exclusions are deferred with production. | Formal performance acceptance and future production SLA validation, not local feature implementation. |
| D-09 (deferred) | Production deployment is deferred. Revisit provider/budget, backup policy and recovery targets when production is requested. Local Docker Compose and observability remain in scope. | Future production deployment only. |
| D-10-R (resolved) | Retain approved audit events and operational logs for one year, then expire them. No organizational policy is currently defined. This retention does not delay physical deletion of project/task content or set backup/metrics retention. | No pending retention decision. |

## Latest approvals and delegated implementation decisions

- D-01: the customer approved one Project Management module owning projects, tasks, assignments and comments on 2026-10-29, replacing the separate Project and Task boundaries. User and Team remain separate modules; the backend remains one deployable process. Keep distinct internal responsibilities and entities within Project Management. This changes internal organization only; existing API contracts, data ownership by the application/Keycloak, business rules and NFR targets remain unchanged. Module interfaces, remaining physical schema details and missing team/role API operations remain delegated. Use approved array responses without pagination. Reject duplicate/second assignments with HTTP 409; replace via explicit removal then addition.
- D-02: Administrators assign exactly one role per account. Emails are unique case-insensitively. Keycloak owns identity/email; application owns team membership. No email/profile editing in MVP. Provisioning succeeds only when both identity and application records are ready; partial failures require retry/reconciliation without duplicate accounts.
- D-03: delete only teams without users or projects; otherwise HTTP 409. Membership always has exactly one team.
- D-05: remove invalid assignments when a role changes, a user/project changes teams, or a task moves projects. For application-owned changes, cleanup is part of the same transaction. Keycloak role changes require coordinated reconciliation and access invalidation.
- D-10: technical security settings delegated; see architecture.md. Logout immediately invalidates API access for the logged-out session, across replicas; do not wait for JWT expiry.
- D-11: Administrators manage users/teams/roles only and cannot access project/task content. Project Managers and Software Engineers read comments; only Software Engineers post them. Project owner records the creator; no transfer workflow.
- D-12: concrete local topology delegated; public authentication, API and operational routes must not bypass the shared limiter. Deployment-specific trusted proxies are configuration, not a new business rule.
- D-14: Spring Boot applications include Actuator, Micrometer and OTLP export. Compose includes Grafana and Loki. Prometheus is selected under delegated topology work as the OTLP metrics store; Loki stores logs and Grafana queries both. Exact compatible versions must be pinned and verified at implementation.

## Scope and evidence

MVP: login/logout/profile, projects/tasks with physical deletion, zero-or-one Software Engineer assignee, comments, project-name search, task filters and home task counts; administrator user/team management. Backlogs, Kanban, general analytics, external integrations, public registration and password recovery remain outside scope.

The approved API baseline in openapi.yaml has been revised to reflect these decisions. The PNG wireframe predates the single-assignee decision and is illustrative only; current textual design notes take precedence. No attachment was available for this update; role mapping uses the user instructions and existing Markdown requirements.
