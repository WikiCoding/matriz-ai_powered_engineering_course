# Matriz requirements

## Vision and revision

Provide a simple English-language application for teams to manage projects and tasks. On 2026-10-29 the customer adopted Practical Assignment 03, superseding conflicting original MVP rules. Its implementation exercises are reference material; this revision covers modeling and architecture only.

## User types and access

- **Administrator:** creates and views users and teams; assigns each user one role and one team at creation. Administrators have no project/task content access.
- **Project Manager:** creates projects and manages projects/tasks they are authorized to access.
- **Software Engineer:** retains read-only project/task management permissions within accessible resources; only this role may add comments; Project Managers and Software Engineers can read comments.

Projects have an owner derived from the authenticated creator. Backend checks must enforce resource access on projects and their tasks, comments and assignments, including direct requests with changed identifiers. All Project Managers and Software Engineers can access all projects regardless of owner or team. Administrators have no project/task content access; ownership records the creator and cannot be transferred.

## Functional requirements

- **FR-01 Authentication:** Email/password login through Keycloak, logout and authenticated-profile retrieval; invalid/expired sessions receive understandable UI handling.
- **FR-02 Roles:** Administrator, Project Manager and Software Engineer roles are assigned at account creation, owned by Keycloak and enforced by the backend; no post-creation role-change workflow is in scope.
- **FR-03 User and team administration:** Administrators create and view/list users and teams. At user creation, assign exactly one Keycloak role and exactly one existing team. User updates and deletion were not requested; this includes email/profile edits, role changes and team-membership changes after creation. Team updates (including renaming) and deletion were not requested. These operations are outside scope. User and team listings are paginated; users sort by email and teams sort alphabetically by name.
- **FR-04 Projects:** Create, list, view and edit accessible projects. Project listings are paginated and sort alphabetically by name. Preserve unique identifier and exactly one team; add authenticated creator as owner. Project name is 3–100 characters; description is at most 1000 characters.
- **FR-05 Project deletion:** Authorized users can delete projects after UI confirmation. Physically cascade deletion to the project's tasks and their comments/assignments; preserve users and teams and leave no orphans. This replaces archival-only behavior.
- **FR-06 Tasks:** Create, list, view, edit and delete tasks belonging to an existing accessible project. Physical task deletion also deletes its comments and assignment; users and teams remain.
- **FR-07 Task data:** Unique identifier, title, description, effort, creation/last-edit dates, status, priority, project, derived team and zero or one eligible Software Engineer assignee. Title is required, 3–150 characters; description is at most 2000 characters.
- **FR-08 Task rules:** Preserve effort values `1, 2, 3, 5, 8, 13`. Adopt statuses `TODO`, `IN_PROGRESS`, `DONE` (default `TODO`) and priorities `LOW`, `MEDIUM`, `HIGH` (default `MEDIUM`). These statuses replace `NOT_STARTED`, `ON_GOING`, `QA`, `DONE`; priority is separate from effort. Reject blank/whitespace-only names and titles and invalid enum values.
- **FR-09 Team consistency:** A task derives its team from its project, including after a project/team change.
- **FR-10 Authorization:** Enforce both role permissions and project accessibility on every operation. A client-supplied owner/author is not proof of identity. Enforce the role mapping below; do not infer access from assignee or team membership alone.
- **FR-11 Assignees:** Associate zero or one Software Engineer from the project's team with a task. Project Managers add/remove assignments. Prevent duplicate task/user pairs; validate existence and eligibility in the backend. Remove invalid assignments atomically on project-team or task-project changes. Users retain their creation-time role and team; user changes are not an assignment-cleanup workflow. Reject a second assignment with HTTP 409; replacement requires removal followed by addition.
- **FR-12 Comments:** Add and list persistent task comments with content, author and date. Content is 1–2000 characters and not whitespace-only. Backend determines author from authentication and date from its clock. Only Software Engineers may add comments; Project Managers and Software Engineers may read them; individual comment editing/deletion was not requested and is outside scope. Comments are still physically deleted when their parent task or project is deleted.
- **FR-13 Filters and home panel:** Task listings are paginated, sort by creation date, and filter by status and priority. Show authenticated user, accessible projects and task counts by status with detail navigation. Counts reflect the relevant authorized dataset, not just a displayed page. General analytics remain out of scope.
- **FR-14 UI consistency:** Refresh lists, filters and counts after confirmed changes without manual reload. Provide loading, empty, success and error states; retain entered data on failed submissions and prevent repeated submissions while pending. Support different screen widths and shared UI components.

Field lengths, status/priority values and defaults above adopt the assignment's example rules as the revised modeling baseline. Collection listings require pagination; the precise API pagination contract remains delegated under D-01.

## Non-functional requirements

- **NFR-01 Language:** English application and documentation.
- **NFR-02 Capacity:** Approximately 500 users.
- **NFR-03 Local environment:** Docker Compose for all application and supporting services, with persistent data across restarts.
- **NFR-04 Deployment:** Production is deferred. Current delivery targets the complete local Docker Compose environment; production provider and recovery choices will be revisited later.
- **NFR-05 Availability:** Confirmed **99.95% SLA**; measurement scope/window and exclusions remain D-08.
- **NFR-06 Rate limiting:** Every endpoint must be rate-limited by client IP, including unauthenticated requests. The quota is 5 requests/second per IP with Redis; quota excess or limiter outage returns HTTP 429. Local topology completion is delegated under D-12.
- **NFR-07 Structure:** Java 21/Spring Modulith backend with Clean Architecture and separate User, Team and Project Management modules in one deployable backend. Project Management contains projects, tasks, assignments and comments with distinct internal responsibilities. React frontend with React Router and reusable components; see architecture.md.
- **NFR-08 Verification:** Automated validation, authorization and creation/read-back persistence coverage; unit, integration and browser tests as specified in architecture.md. No tests have run yet.

## Scope and unresolved details

The MVP includes all FRs above, subject to the explicit rules still requiring decisions in [constraints](constraints.md). Backlogs, Kanban, general reporting/analytics, external business integrations, public registration and password recovery are not included. The [data model](database.md) expresses the revised logical design. The API baseline is approved and revised for the latest customer decisions. The [revised wireframe](ui/wireframe.png) illustrates the current scope without resolving pending decisions.

## Role mapping and latest approved rules

Each user has exactly one application role, assigned at creation by an Administrator in Keycloak. No separate local role authority or implicit role inheritance is introduced.

| Keycloak role     | Authorized responsibility (subject to resource access)                                                                                                                       |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ADMINISTRATOR     | Create and view/list users and teams; assign exactly one role and one team per user at creation; no subsequent user/team update or deletion; no project/task content access. |
| PROJECT_MANAGER   | Create/manage accessible projects and tasks; manage task assignment to zero or one Software Engineer. May read comments, but not create them.                                |
| SOFTWARE_ENGINEER | Read accessible projects/tasks; add comments; eligible to be the single optional assignee. No project/task mutation or assignment management.                                |

Project/task UUIDs are internal resource identifiers; omit separate business identifiers and UI identifier fields. Physically cascade project/task deletion to dependent tasks, comments and assignments. Timestamps use the server zone and 24-hour `dd/MM/yyyy HH:mm:ss`. shadcn/ui is permitted. The latest decisions supersede the assignment's previous multiple-assignee baseline.

Administrators communicate credentials in person. New accounts are email verified and can log in with the supplied password without a first-login password change; sessions end after an absolute maximum of eight hours and require login again. Access-token lifetime is separate. IP limiting uses a global Redis token bucket per IP across routes/instances, five tokens/second and capacity five. Forwarded IP headers are trusted only from configured proxies; other callers cannot override their source IP. Flyway manages application database migrations.

## Additional approved requirements

Administrator registration requires first and last names. Both are stored in the Keycloak identity and must be nonblank so the new account can complete direct login without a profile-update action.

- **FR-15 Provisioning:** emails are unique without case sensitivity; Keycloak owns identity/email and the application owns team membership. Complete both records before reporting success; reconcile partial failures. All post-creation user updates and deletion are excluded. Compensation for failed provisioning remains part of creation, not an account-deletion feature.
- **FR-16 Logout:** invalidate the current application session and its API access immediately across replicas. Existing bearer tokens must not bypass session revocation.
- **NFR-09 Performance:** p95 response time under two seconds. Measurement boundary, concurrent load and dataset size must be documented for formal acceptance under D-08; approximately 500 registered users does not specify concurrent load.
- **NFR-10 Observability:** the backend, including its integrated HTTP edge, includes Spring Actuator, Micrometer and OTLP export. Compose includes Grafana, Loki and Prometheus for metrics storage under delegated topology choices. Observe HTTP latency/errors, JVM, database pools and limiter health.
- **NFR-11 (revoked 2026-10-30):** Business audit records are not part of this project. Do not add dedicated audit storage. Operational logging and observability remain governed by NFR-10, with no fixed retention period required for the local MVP.


## Superseding decision D-16 (2026-10-30)

The customer removed backend session tracking and IP rate limiting. This supersedes all earlier Redis, limiter, immediate logout, back-channel revocation and immediate role-change invalidation requirements above. Redis is removed from the backend dependencies and Compose. Keycloak retains its own sessions and credential protections. The backend validates JWTs statelessly; logout terminates the Keycloak session, while issued access tokens and role claims remain usable until expiry (five minutes plus validator clock skew). Clients discard tokens on logout. Provider logout failure is reported and can be retried.

Valid tokens require no backend session registration. Missing, invalid and expired tokens remain rejected. Obsolete limiter tests were removed and security tests adjusted. Tests and Docker validation were not run, as requested; prior verification results apply only to the previous implementation. Legacy gateway source remains historical and is not deployed. Existing Redis volumes were not deleted.
