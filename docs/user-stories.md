# MVP user stories

All stories are **Must** for the revised MVP adopted on 2026-10-29. Criteria are intended future checks, not executed results. Decision IDs refer to [constraints](constraints.md); unresolved details block only affected behavior. All Project Managers and Software Engineers can access all projects, and deletion replaces the old archive-only story.

## US-01.1 — Create a user

As an Administrator, I want to provision a user with email and a Keycloak role.

1. Valid required details create an account through the agreed Keycloak provisioning flow and a linked local profile.
2. Missing/invalid details produce an understandable error without claiming successful creation.
3. Credentials and role authority remain in Keycloak.

Emails are unique case-insensitively; exactly one Administrator-assigned role and one team are required. Partial provisioning must not report success or create duplicates on retry. No email/profile editing.

## US-01.2 — Manage team membership

As an Administrator, I want to assign or move users between teams.

1. Successful changes appear in the user's membership.
2. Every user retains exactly one team; remove now-invalid assignments atomically. Reject deletion of teams with users/projects.

## US-01.3 — Sign in, inspect my profile and sign out

As a user, I want a complete authenticated session.

1. Valid email/password credentials authenticate through Keycloak; invalid credentials and unauthenticated API requests are rejected.
2. Profile retrieval reflects the verified authenticated user.
3. After logout succeeds, subsequent API requests from that session are rejected immediately on every replica, including replay of its access token. Expired sessions lead to login with an understandable message.

Verify both UI and direct API requests, including session and token replay after logout.

## US-02.1 — Create, find and edit accessible projects

As a Project Manager, I want to manage projects I am authorized to access.

1. A valid project has a unique identifier, name, description according to the adopted contract, one team and an owner derived from the authenticated creator.
2. Names shorter than 3 or longer than 100 characters, whitespace-only names and descriptions longer than 1000 characters are rejected.
3. Lists and name searches return only accessible projects. Another user's identifier does not bypass authorization.
4. Authorized changes persist after reload and backend restart; changing the project team changes every task's displayed team.

Technical contract completion is delegated under D-01. Use UUIDs only; all Project Managers and Software Engineers have project access.

## US-02.2 — Delete a project

As an authorized Project Manager, I want to delete a project.

1. The UI requests confirmation before submitting deletion; cancellation does not delete.
2. Backend authorization is checked independently of the UI.
3. Deletion follows the agreed dependent-record/retention policy, leaves no orphans and updates the list and counts after success.
4. Failure shows an error, without presenting the deletion as successful.

Deletion physically cascades to tasks, comments and assignments; preserve users and teams.

## US-03.1 — Create and view a task

As an authorized Project Manager, I want persistent tasks in an accessible project.

1. A valid task stores identifier, title, description, effort, status, priority, dates and project; team is derived from project, and assignees are represented by assignments.
2. Title must be 3–150 characters and not whitespace-only; description is at most 2000 characters; effort is one of 1, 2, 3, 5, 8, 13.
3. Status is TODO, IN_PROGRESS or DONE, default TODO; priority is LOW, MEDIUM or HIGH, default MEDIUM. Invalid values are rejected.
4. The created task appears without manual refresh and remains after reopening/restarting the backend.

Use UUID resource IDs and server-zone dd/MM/yyyy HH:mm:ss timestamps. Tasks may be created with no assignee.

## US-03.2 — Edit, filter and delete tasks

As an authorized Project Manager, I want to maintain tasks and filter by status/priority.

1. Lists contain only the selected accessible project's tasks and honor the selected filters.
2. Edits update last-edited time, list contents and relevant counts after API confirmation.
3. Authorized deletion respects the dependent-record policy and leaves no orphans.
4. Validation/API errors retain form data, and pending requests prevent duplicate submissions.

Use physical cascade deletion, server-zone 24-hour timestamps and the approved unpaginated list contract.

## US-03.3 — Manage zero or one Software Engineer assignee

As a Project Manager, I want to assign at most one Software Engineer to a task and remove that assignment.

1. A single optional assignment persists and appears after reopening the task.
2. Duplicate task/user pairs and a second assignee are rejected, including concurrent requests.
3. Nonexistent or ineligible users cannot be assigned; removing the assignment leaves the task unassigned and does not delete the user.

Tasks may have zero assignees. Only Software Engineers qualify and only Project Managers manage assignment. Assignees must belong to the project team. Moving a user to a new team automatically removes their old-team assignments atomically; role changes, project-team changes and task moves also remove assignments that become invalid.

## US-03.4 — Add and read comments

As a Software Engineer permitted to access the task, I want to add and read task comments.

1. Content is 1–2000 characters and cannot consist only of whitespace.
2. Backend records the authenticated author and date; supplied author values cannot impersonate another user.
3. Comments remain available after reopening the task and are visible only with the required project/task access.

Use server-zone 24-hour timestamps. Project Managers can read comments; Administrators cannot. Comment editing/deletion is not added.

## US-04.1 — View accessible work

As a Software Engineer, I want to view the work I am permitted to access.

1. Authorized project/task details can be viewed; inaccessible resources are rejected even with a direct API request.
2. Project/task creation, editing and deletion remain unavailable to this role.
3. Comment and assignment permissions follow the separately agreed matrix; read access alone does not grant them.

All Software Engineers can view all projects/tasks regardless of owner or team; this does not authorize mutations.

## US-04.2 — Use the home panel

As an authenticated user, I want accessible projects and task counts by status with navigation to details.

1. The panel identifies the authenticated user and shows only accessible data.
2. Counts cover the relevant dataset, not only the current page, and update after successful changes.
3. Navigation uses React Router; shared controls and feedback states use reusable components.
4. Loading, empty, success and error states remain understandable at different screen widths.

Administrators receive their administration navigation without project/task content; this summary does not add general analytics.

## Cross-cutting verification

Verify validation, unauthorized resource access and create/read-back persistence with automated tests. Cover IP limiting for every endpoint (including unauthenticated traffic), with quota, spoofed forwarding-header and multi-instance scenarios using the approved five-token bucket/refill and trusted-proxy policy. Availability checks require the D-08 measurement definition; the target is confirmed at 99.95%. See architecture.md for Mockito, Spring Boot/Testcontainers, Jest and Playwright responsibilities.

## Decision revision acceptance checks

Verify exactly one team per user; administrator-only provisioning/team administration; zero-or-one Software Engineer assignee enforced under concurrency; Project-Manager-only assignment mutation; Software-Engineer-only comment creation; UUID-only identifiers with no UI identifier fields; physical cascade deletion without orphans; server-zone dd/MM/yyyy HH:mm:ss timestamp formatting; 5 requests/second per IP with Redis and HTTP 429 on limiter outage. Dependency references above refer only to the remaining portions in constraints.md.

Verify mandatory first-login password change, eight-hour absolute session expiry and reauthentication, same-team assignment rejection, atomic old-team unassignment on user moves, shared cross-route/replica rate counters and Redis outage HTTP 429. Migration tests must apply Flyway to an empty PostgreSQL database and upgrade an existing one without losing retained records.

## Final decision acceptance checks

Verify Administrator-only role changes with exactly one role; no Administrator project/task/comment access; Project Manager comment reads and rejected comment posts; no ownership-transfer or email/profile-edit workflow; HTTP 409 for occupied-team deletion and second assignments; cleanup after role/project/task moves; case-insensitive duplicate emails and recovery from partial provisioning.

Replay the logged-out session cookie and bearer token on each replica immediately after successful logout; both must fail. Cover in-flight requests separately: logout prevents subsequent authorization, but cannot undo a transaction already committed.

Verify gateway/backend metrics and logs arrive in Grafana through their respective stores, no secrets/content leak to telemetry, and p95 latency is measurable against the under-two-second NFR. Test one-year audit/operational-log retention and expiration using controlled timestamps, independently of immediate content deletion. Load-test conditions remain D-08; production and its SLA acceptance are deferred.
