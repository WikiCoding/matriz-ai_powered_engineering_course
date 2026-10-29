# MVP user stories

Priority: **Must** = required for the MVP.

Decision IDs refer to the authoritative [constraints register](constraints.md). Acceptance criteria involving unresolved decisions remain blocked for the affected behavior.

## EP-01 — User and team administration

### US-01.1 — Create a user — Must

As an Administrator, I want to create a user with credentials and a role, so that the person can access Matriz with the appropriate permissions.

1. Given an Administrator, when they create a user with email, password, and role, then a sign-in account with that email and role is provisioned through the approved identity flow (D-02, D-10).
2. Given a saved account, when the user signs in with its email and password, then the user is authenticated under the saved role.
3. Given an Administrator, when a required value is missing, then no account is created and the missing value is identified.

**Edge cases:** Missing email, password, or role; identity and uniqueness rules depend on D-02 and D-10.

### US-01.2 — Manage team membership — Must

As an Administrator, I want to assign a user to a team or move them to another team, so that team membership remains current.

1. Given an Administrator, a user, and a team, when the Administrator assigns the user, then the user is shown in that team.
2. Given a user in Team A, when the Administrator changes the user's team to Team B, then the user is shown in Team B.

**Dependency:** D-03 governs membership cardinality and team lifecycle.

## EP-02 — Project management

### US-02.1 — Create and edit a project — Must

As a Project Manager, I want to create and edit a project with one team, so that tasks can be organized by project and team.

1. Given a Project Manager and an existing team, when they provide a unique identifier, name, description, and team, then a project is saved with those values.
2. Given a Project Manager, when a required project value is missing, then no project is created and the missing value is identified.
3. Given a project with tasks, when its team changes, then each task displays the new project team.

**Edge cases:** Identifier handling depends on D-04; Project Managers can manage projects from all teams.

### US-02.2 — Archive a project — Must

As a Project Manager, I want to archive a project, so that it is no longer active without permanently removing its information.

1. Given a Project Manager and a project, when the Project Manager archives it, then the project is marked as archived.
2. Given an archived project, then its saved project data and associated tasks are retained; access and edit behavior depend on D-06.

**Dependency:** D-06 governs archival behavior.

## EP-03 — Task management

### US-03.1 — Create a task — Must

As a Project Manager, I want to create a task in a project, so that work is recorded and visible to the project team.

1. Given a Project Manager and a project, when they provide a unique identifier, title, description, allowed effort, status, and a Software Engineer assignee, then the task is saved in that project.
2. Given a saved task, when its details are viewed, then its identifier, title, description, effort, creation date, last edited date, status, project, team, and assignee are displayed.
3. Given a task request, when effort is not `1`, `2`, `3`, `5`, `8`, or `13`, then the task is not saved.

**Dependencies:** D-04 governs identifiers, D-05 assignment eligibility, and D-07 timestamps.

### US-03.2 — View and edit a task — Must

As a Project Manager, I want to view and edit a task, so that task information and progress remain current.

1. Given a project with tasks, when the Project Manager opens its task list, then only tasks associated with that project are shown.
2. Given a task, when the Project Manager changes a task field, then the saved task displays the new value and its last edited date is updated.
3. Given a task status update, when the Project Manager selects a status outside `NOT_STARTED`, `ON_GOING`, `QA`, or `DONE`, then the update is rejected.

**Edge cases:** Project changes must update the displayed team; assignment, archival, and timestamp behavior depend on D-05, D-06, and D-07.

## EP-04 — Read-only work visibility

### US-04.1 — View work information — Must

As a Software Engineer, I want to view teams, projects, and tasks, so that I can understand the work assigned to me and my organization.

1. Given a Software Engineer, when they access Matriz, then they can view teams, projects, task lists, and task details.
2. Given a Software Engineer, when they attempt to change a user, team, project, or task, then the system does not perform the change.

**Edge cases:** Software Engineers can view all teams and projects, not only those of their own team.
