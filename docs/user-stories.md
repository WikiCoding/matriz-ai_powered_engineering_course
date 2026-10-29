# MVP user stories

All stories are **Must** for the revised MVP adopted on 2026-10-29. Criteria are intended future checks, not executed results. Decision IDs refer to [constraints](constraints.md); unresolved details block only affected behavior. Project access is no longer global, and deletion replaces the old archive-only story.

## US-01.1 — Create a user

As an Administrator, I want to provision a user with email and a Keycloak role.

1. Valid required details create an account through the agreed Keycloak provisioning flow and a linked local profile.
2. Missing/invalid details produce an understandable error without claiming successful creation.
3. Credentials and role authority remain in Keycloak.

Dependencies: provisioning, email uniqueness and security policy (D-02, D-10).

## US-01.2 — Manage team membership

As an Administrator, I want to assign or move users between teams.

1. Successful changes appear in the user's membership.
2. Membership cardinality and resulting assignment/access effects follow D-03, D-05 and D-11.

## US-01.3 — Sign in, inspect my profile and sign out

As a user, I want a complete authenticated session.

1. Valid email/password credentials authenticate through Keycloak; invalid credentials and unauthenticated API requests are rejected.
2. Profile retrieval reflects the verified authenticated user.
3. Logout ends access according to the agreed session/token lifecycle; expired sessions lead to login with an understandable message.

Dependencies: D-02, D-10. Verify both UI and direct API requests.

## US-02.1 — Create, find and edit accessible projects

As a Project Manager, I want to manage projects I am authorized to access.

1. A valid project has a unique identifier, name, description according to the adopted contract, one team and an owner derived from the authenticated creator.
2. Names shorter than 3 or longer than 100 characters, whitespace-only names and descriptions longer than 1000 characters are rejected.
3. Lists and name searches return only accessible projects. Another user's identifier does not bypass authorization.
4. Authorized changes persist after reload and backend restart; changing the project team changes every task's displayed team.

Dependencies: identifiers D-04; access/sharing/ownership transfer D-11; detailed contract D-01.

## US-02.2 — Delete a project

As an authorized Project Manager, I want to delete a project.

1. The UI requests confirmation before submitting deletion; cancellation does not delete.
2. Backend authorization is checked independently of the UI.
3. Deletion follows the agreed dependent-record/retention policy, leaves no orphans and updates the list and counts after success.
4. Failure shows an error, without presenting the deletion as successful.

Dependencies: deletion policy D-06 and resource permissions D-11. Archival is no longer the required substitute.

## US-03.1 — Create and view a task

As an authorized Project Manager, I want persistent tasks in an accessible project.

1. A valid task stores identifier, title, description, effort, status, priority, dates and project; team is derived from project, and assignees are represented by assignments.
2. Title must be 3–150 characters and not whitespace-only; description is at most 2000 characters; effort is one of 1, 2, 3, 5, 8, 13.
3. Status is TODO, IN_PROGRESS or DONE, default TODO; priority is LOW, MEDIUM or HIGH, default MEDIUM. Invalid values are rejected.
4. The created task appears without manual refresh and remains after reopening/restarting the backend.

Dependencies: identifiers D-04, minimum assignment count D-05, timestamps D-07 and access D-11.

## US-03.2 — Edit, filter and delete tasks

As an authorized Project Manager, I want to maintain tasks and filter by status/priority.

1. Lists contain only the selected accessible project's tasks and honor the selected filters.
2. Edits update last-edited time, list contents and relevant counts after API confirmation.
3. Authorized deletion respects the dependent-record policy and leaves no orphans.
4. Validation/API errors retain form data, and pending requests prevent duplicate submissions.

Dependencies: D-06, D-07, D-11; pagination is only required if adopted in the API contract.

## US-03.3 — Manage multiple assignees

As an authorized user, I want to add eligible users to a task and remove individual assignments.

1. Multiple assignments persist and appear after reopening the task.
2. Duplicate task/user pairs are rejected, including concurrent requests.
3. Nonexistent or ineligible users cannot be assigned; removing one assignment does not remove other assignments or delete the user.

Dependencies: eligibility/minimum count D-05 and assignment-management permissions D-11. This replaces the single-assignee rule.

## US-03.4 — Add and read comments

As a user permitted to comment, I want to add and read task comments.

1. Content is 1–2000 characters and cannot consist only of whitespace.
2. Backend records the authenticated author and date; supplied author values cannot impersonate another user.
3. Comments remain available after reopening the task and are visible only with the required project/task access.

Dependencies: timestamp representation D-07 and comment permissions D-11. Comment editing/deletion is not added.

## US-04.1 — View accessible work

As a Software Engineer, I want to view the work I am permitted to access.

1. Authorized project/task details can be viewed; inaccessible resources are rejected even with a direct API request.
2. Project/task creation, editing and deletion remain unavailable to this role.
3. Comment and assignment permissions follow the separately agreed matrix; read access alone does not grant them.

Dependency: D-11. The former visibility of all projects and teams is superseded.

## US-04.2 — Use the home panel

As an authenticated user, I want accessible projects and task counts by status with navigation to details.

1. The panel identifies the authenticated user and shows only accessible data.
2. Counts cover the relevant dataset, not only the current page, and update after successful changes.
3. Navigation uses React Router; shared controls and feedback states use reusable components.
4. Loading, empty, success and error states remain understandable at different screen widths.

Dependency: D-11; this summary does not add general analytics.

## Cross-cutting verification

Verify validation, unauthorized resource access and create/read-back persistence with automated tests. Cover IP limiting for every endpoint (including unauthenticated traffic), with quota, spoofed forwarding-header and multi-instance scenarios after D-12 is resolved. Availability checks require the D-08 measurement definition; the target is confirmed at 99.95%. See architecture.md for Mockito, Spring Boot/Testcontainers, Jest and Playwright responsibilities.
