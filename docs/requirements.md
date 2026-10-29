# Matriz requirements

## Vision and revision

Provide a simple English-language application for teams to manage projects and tasks. On 2026-10-29 the customer adopted Practical Assignment 03, superseding conflicting original MVP rules. Its implementation exercises are reference material; this revision covers modeling and architecture only.

## User types and access

- **Administrator:** creates users and manages team membership. Additional project access is not implied.
- **Project Manager:** creates projects and manages projects/tasks they are authorized to access.
- **Software Engineer:** retains read-only project/task management permissions within accessible resources; comment permissions remain to be defined.

Projects have an owner derived from the authenticated creator. Backend checks must enforce resource access on projects and their tasks, comments and assignments, including direct requests with changed identifiers. Sharing, ownership transfer and remaining role permissions are pending under D-11; the former all-project/all-team visibility rule is superseded.

## Functional requirements

- **FR-01 Authentication:** Email/password login through Keycloak, logout and authenticated-profile retrieval; invalid/expired sessions receive understandable UI handling.
- **FR-02 Roles:** Administrator, Project Manager and Software Engineer roles are managed by Keycloak and enforced by the backend.
- **FR-03 User administration:** Administrators create users and manage team membership through the approved provisioning flow (D-02, D-03).
- **FR-04 Projects:** Create, list, view, edit and search accessible projects by name. Preserve unique identifier and exactly one team; add authenticated creator as owner. Project name is 3–100 characters; description is at most 1000 characters.
- **FR-05 Project deletion:** Authorized users can delete projects after UI confirmation. Dependent-record behavior must be defined under D-06; no orphaned records are allowed. This replaces archival-only behavior.
- **FR-06 Tasks:** Create, list, view, edit and delete tasks belonging to an existing accessible project. Deletion must respect documented dependencies (D-06).
- **FR-07 Task data:** Unique identifier, title, description, effort, creation/last-edit dates, status, priority, project, derived team and multiple eligible assignees. Title is required, 3–150 characters; description is at most 2000 characters.
- **FR-08 Task rules:** Preserve effort values `1, 2, 3, 5, 8, 13`. Adopt statuses `TODO`, `IN_PROGRESS`, `DONE` (default `TODO`) and priorities `LOW`, `MEDIUM`, `HIGH` (default `MEDIUM`). These statuses replace `NOT_STARTED`, `ON_GOING`, `QA`, `DONE`; priority is separate from effort. Reject blank/whitespace-only names and titles and invalid enum values.
- **FR-09 Team consistency:** A task derives its team from its project, including after a project/team change.
- **FR-10 Authorization:** Enforce both role permissions and project accessibility on every operation. A client-supplied owner/author is not proof of identity. D-11 resolves remaining permissions; do not infer access from assignee or team membership alone.
- **FR-11 Assignees:** Associate multiple eligible users with a task and remove individual assignments. Prevent duplicate task/user pairs; validate existence and eligibility in the backend (D-05, D-11).
- **FR-12 Comments:** Add and list persistent task comments with content, author and date. Content is 1–2000 characters and not whitespace-only. Backend determines author from authentication and date from its clock. Comment permissions depend on D-11; editing/deleting comments is not added to scope.
- **FR-13 Filters and home panel:** Filter tasks by status and priority. Show authenticated user, accessible projects and task counts by status with detail navigation. Counts reflect the relevant authorized dataset, not just a displayed page. General analytics remain out of scope.
- **FR-14 UI consistency:** Refresh lists, filters and counts after confirmed changes without manual reload. Provide loading, empty, success and error states; retain entered data on failed submissions and prevent repeated submissions while pending. Support different screen widths and shared UI components.

Field lengths, status/priority values and defaults above adopt the assignment's example rules as the revised modeling baseline; pagination is not assumed and must be settled in the API contract.

## Non-functional requirements

- **NFR-01 Language:** English application and documentation.
- **NFR-02 Capacity:** Approximately 500 users.
- **NFR-03 Local environment:** Docker Compose for all application and supporting services, with persistent data across restarts.
- **NFR-04 Deployment:** Provider undecided; AWS is a candidate.
- **NFR-05 Availability:** Confirmed **99.95% SLA**; measurement scope/window and exclusions remain D-08.
- **NFR-06 Rate limiting:** Every endpoint must be rate-limited by client IP, including unauthenticated requests. Thresholds and deployment details remain D-12.
- **NFR-07 Structure:** Java 21/Spring Modulith backend with Clean Architecture; React frontend with React Router and reusable components; see architecture.md.
- **NFR-08 Verification:** Automated validation, authorization and creation/read-back persistence coverage; unit, integration and browser tests as specified in architecture.md. No tests have run yet.

## Scope and unresolved details

The MVP includes all FRs above, subject to the explicit rules still requiring decisions in [constraints](constraints.md). Backlogs, Kanban, general reporting/analytics, external business integrations, public registration and password recovery are not included. The [data model](database.md) expresses the revised logical design. The unchanged API contract needs later reconciliation. The [revised wireframe](ui/wireframe.png) illustrates the current scope without resolving pending decisions.
