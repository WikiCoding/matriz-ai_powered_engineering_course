# Matriz requirements

## Vision and objectives

Provide a simple, English-language web application for work teams to manage projects and their task lists. The MVP focuses on reliable task visibility and controlled updates, not full project-management tooling.

## User types

| User type         | Permitted actions                                                                    |
|-------------------|--------------------------------------------------------------------------------------|
| Administrator     | Create users; assign users to teams; change a user's team.                           |
| Project Manager   | View all projects; create, edit, and archive projects; create, view, and edit tasks. |
| Software Engineer | View teams, projects, task lists, and task details only.                             |

## Functional requirements

- **FR-01 Authentication:** Users sign in with email and password.
- **FR-02 Roles:** The system supports Administrator, Project Manager, and Software Engineer roles.
- **FR-03 User administration:** Administrators create users and manage their team membership.
- **FR-04 Projects:** A Project Manager manages projects across all teams. A project has a unique identifier, name, description, and exactly one team.
- **FR-05 Project archival:** Projects are archived rather than permanently deleted.
- **FR-06 Tasks:** A Project Manager creates, views, and edits tasks associated with projects.
- **FR-07 Task data:** A task includes a unique identifier, title, description, effort, creation date, last edited date, status, project, team, and one Software Engineer assignee.
- **FR-08 Task rules:** Effort is one of `1, 2, 3, 5, 8, 13`; status is `NOT_STARTED`, `ON_GOING`, `QA`, or `DONE`.
- **FR-09 Team consistency:** A task displays its project's team. Changing a project's team updates the team shown on every task in that project.
- **FR-10 Read-only access:** Software Engineers cannot create, edit, archive, assign, or change users, teams, projects, or tasks.

## Non-functional requirements

- **NFR-01 Language:** Application and documentation are in English.
- **NFR-02 Capacity target:** Plan for approximately 500 users.
- **NFR-03 Containerization:** The application is containerized with Docker.
- **NFR-04 Deployment:** The application is deployable on AWS.
- **NFR-05 Availability (proposed):** A 99.95% SLA has been requested but conflicts with the previously pending availability decision; confirmation and measurement scope are tracked in [constraints](constraints.md) (D-08).

## MVP scope

The MVP is limited to a list of tasks per project, project and task management by Project Managers, user/team administration by Administrators, and read-only visibility for Software Engineers.

## Outside the initial scope

- Backlogs.
- Kanban boards or other board views.
- Reports and analytics.
- Integrations.
- Permanent deletion.

## Decisions and design artifacts

The [constraints register](constraints.md) lists pending decisions and the work each blocks. Architecture, data model, and API contract documents are design proposals supporting the MVP; their existence does not approve implementation choices or expand product scope.
