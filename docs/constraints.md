# Constraints and pending decisions

## Confirmed decisions

Customer decisions on 2026-10-29 supersede conflicting earlier drafts.

- English application/documentation; approximately 500 users.
- Java 21, Spring Boot/Spring Modulith modular monolith and Clean Architecture; integrated HTTP edge in the same Spring MVC backend process (customer simplification on 2026-10-30).
- Keycloak owns credentials and roles; PostgreSQL owns application data.
- Docker Compose runs the full local environment, including Redis, Grafana, Loki and the selected Prometheus metrics store.
- React, React Router, local state and optional useContext; no Flux, useReducer or Redux Toolkit. Custom CSS remains selected; Tailwind CSS and shadcn/ui are permitted alternatives. shadcn/ui may use Tailwind styling; permission to use it does not prohibit that dependency.
- Mockito, Spring Boot/Testcontainers, Jest and Playwright are selected.
- Availability target is 99.95%; production hosting is deferred. Response-time target is p95 under two seconds. Business audit records are out of scope, and no fixed operational-log retention period is required.
- D-01: existing schema design and API contracts are approved, subject to the explicit revisions below. The repository contains a logical model, not a detailed physical DDL specification. Completion of detailed module interfaces is now delegated.
- D-02: Administrators create accounts and communicate passwords in person. New accounts are email verified and may log in directly with the supplied password; a first-login password change is not required (customer revision 2026-10-31). Keycloak role mapping is in requirements.md. Sessions have an absolute maximum of eight hours, then require login again; access tokens need not last eight hours.
- D-03: each user belongs to exactly one team assigned at creation; Administrators create and view users and teams. D-17 excludes subsequent updates and deletion.
- D-04 (resolved): UUID resource IDs only for projects/tasks; no separate incremental identifiers or visible identifier fields.
- D-05: zero or one assignee, who must be a Software Engineer in the project's team. Users keep their creation-time team; no user-move workflow is in scope (D-17).
- D-06 (resolved): physical cascade deletion. Project deletion removes its tasks and their comments/assignments; task deletion removes its comments/assignment. Preserve users and teams; leave no orphans.
- D-07 (resolved): server timezone with 24-hour `dd/MM/yyyy HH:mm:ss`. Configure application instances consistently.
- D-11: only Software Engineers create comments; Project Managers and Software Engineers read comments; Project Managers manage assignments. All Project Managers and Software Engineers can access all projects, regardless of ownership or team. Resource/role checks still apply.
- D-12: one Redis token bucket per client IP shared across routes and instances; refill five tokens/second, capacity five, one token per request. This is an average rate with a small burst, not a strict rolling-window count. Trust forwarded IP headers only from explicitly configured proxies; ignore them otherwise. Quota exhaustion or limiter outage returns HTTP 429.
- D-13 (resolved, delegated selection): Flyway with versioned PostgreSQL SQL migrations; see architecture.md.
- The original 2026-10-29 revision authorized documentation and contract changes. Subsequent implementation requests, including D-15, authorize their stated application scope.

## Remaining decision register

Registration requires nonblank first and last names (customer revision 2026-10-31). The backend supplies them to Keycloak so the verified account has a complete profile for direct login.

Approvals of proposals 1–8 settle business behavior and delegate technical security/schema/interface completion. Do not reopen delegated implementation details as customer blockers. Production is now explicitly deferred, business audit storage and fixed log retention are out of scope, and the response-time percentile is p95. No unresolved business-rule decision blocks starting local implementation.

| ID                          | Remaining decision                                                                                                                                                                                                                                   | Blocks                                                                                                |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| D-08                        | p95 response time must be under two seconds. Measurement boundary, concurrent load and dataset size still need a documented test scenario. SLA window, scope and exclusions are deferred with production.                                            | Formal performance acceptance and future production SLA validation, not local feature implementation. |
| D-09 (deferred)             | Production deployment is deferred. Revisit provider/budget, backup policy and recovery targets when production is requested. Local Docker Compose and observability remain in scope.                                                                 | Future production deployment only.                                                                    |
| D-10-R (revoked 2026-10-30) | The customer revoked NFR-11. Do not store business audit records and do not impose a fixed operational-log retention period for the local MVP. Ordinary operational logging and observability remain in scope without a mandated retention duration. | No pending retention decision.                                                                        |

## Latest approvals and delegated implementation decisions

- D-15 (2026-10-30): customer requested merging `matriz-api-gateway` into `matriz-backend` for one Java deployment unit. This supersedes the separate reactive gateway topology. Keep the backend MVC/JPA stack; integrate authentication, session checks, CORS and Redis IP limiting as HTTP infrastructure, retaining User, Team and Project Management ownership. Keycloak, PostgreSQL and Redis remain supporting services. No HTTP forwarding to a separate backend process is needed.


- D-01: the customer approved one Project Management module owning projects, tasks, assignments and comments on 2026-10-29, replacing the separate Project and Task boundaries. User and Team remain separate modules; the backend remains one deployable process. Keep distinct internal responsibilities and entities within Project Management. This changes internal organization only; existing API contracts, data ownership by the application/Keycloak, business rules and NFR targets remain unchanged. Module interfaces, remaining physical schema details and technical completion of in-scope team creation/read and initial role assignment remain delegated; D-17 excludes team/role update operations. Collection listings now require pagination: tasks sort by creation date and filter by status; projects and teams sort alphabetically by name; users sort by email. The pagination request/response contract is a delegated implementation detail and the API baseline must be revised before implementing these listings. Reject duplicate/second assignments with HTTP 409; replace via explicit removal then addition.
- D-02: Administrators assign exactly one role per account at creation. Emails are unique case-insensitively. Keycloak owns identity/email; application owns team membership. No post-creation user updates or deletion in MVP (D-17), including email/profile, role and team-membership changes. Provisioning succeeds only when both identity and application records are ready; partial failures require retry/reconciliation without duplicate accounts.
- D-03: team creation and viewing/listing only; team updates and deletion are outside scope (D-17), regardless of whether users/projects reference the team. Membership always has exactly one team assigned at user creation.
- D-05: remove invalid assignments when a project changes teams or a task moves projects, as part of the same transaction. User role/team changes are outside scope (D-17).
- D-10: technical security settings delegated; see architecture.md. Logout immediately invalidates API access for the logged-out session, across replicas; do not wait for JWT expiry.
- D-11: Administrators create and view users/teams and assign roles/team membership at user creation only and cannot access project/task content. Project Managers and Software Engineers read comments; only Software Engineers post them. Project owner records the creator; no transfer workflow.
- D-12: concrete local topology delegated; public authentication, API and operational routes must not bypass the shared limiter. Deployment-specific trusted proxies are configuration, not a new business rule.
- D-14: Spring Boot applications include Actuator, Micrometer and OTLP export. Compose includes Grafana and Loki. Prometheus is selected under delegated topology work as the OTLP metrics store; Loki stores logs and Grafana queries both. Exact compatible versions must be pinned and verified at implementation.

## Scope and evidence

MVP: login/logout/profile, projects/tasks with physical deletion, zero-or-one Software Engineer assignee, comments, paginated/sorted listings, task status/priority filters and home task counts; administrator user/team creation and viewing, with role/team assignment at user creation. Backlogs, Kanban, general analytics, external integrations, public registration and password recovery remain outside scope.

The API baseline in openapi.yaml predates the approved pagination/list-sorting change and must be revised before implementation. The PNG wireframe predates the single-assignee and pagination/list-sorting decisions and is illustrative only; current textual design notes take precedence. No attachment was available for this update; role mapping uses the user instructions and existing Markdown requirements.


## Superseding decision D-16 (2026-10-30)

The customer removed backend session tracking and IP rate limiting. This supersedes all earlier Redis, limiter, immediate logout, back-channel revocation and immediate role-change invalidation requirements above. Redis is removed from the backend dependencies and Compose. Keycloak retains its own sessions and credential protections. The backend validates JWTs statelessly; logout terminates the Keycloak session, while issued access tokens and role claims remain usable until expiry (five minutes plus validator clock skew). Clients discard tokens on logout. Provider logout failure is reported and can be retried.

Valid tokens require no backend session registration. Missing, invalid and expired tokens remain rejected. Obsolete limiter tests were removed and security tests adjusted. Tests and Docker validation were not run, as requested; prior verification results apply only to the previous implementation. Legacy gateway source remains historical and is not deployed. Existing Redis volumes were not deleted.

## D-17 — Customer scope correction (2026-11-01)

The customer clarified that user and team updates and deletion were not requested. The earlier user-lifecycle approval entry and team lifecycle/update/delete provisions overshot the requested application scope and are superseded by this correction.

- Users: creation and viewing/listing only. Assign exactly one role and one existing team at creation. No later email/profile, role or team-membership updates and no account deletion.
- Teams: creation and viewing/listing only. No updates, including renaming, and no deletion, even for empty teams.
- Projects and tasks: retain existing Project Manager update/delete permissions and validation.
- Comments: creation and reading only; no individual edit/delete workflow. Project/task deletion still physically cascades to dependent comments and assignments, preserving users and teams.
- Remove user-deletion anonymization/read-model requirements and cleanup/reconciliation requirements triggered by user role/team changes. Keep assignment eligibility checks and atomic cleanup on project-team changes and task moves.
- Failed account-creation compensation and provisioning retry/reconciliation remain required; these are not an account-deletion feature. No replacement deactivation, archival or administrative correction workflow is introduced.

This approval covers documentation and OpenAPI alignment now. Application code, migrations and implementation tests are unchanged by this refactor; their alignment is not verified or claimed.

## D-18 — Project-name search scope correction (2026-11-01)

The customer did not request project-name search in the source assignment, so it is removed from the MVP. Project listing remains paginated and alphabetically sorted. The old wireframe prompt and PNG are historical illustrations and do not authorize a search control. No backend search route or query is required.
